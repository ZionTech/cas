package org.jasig.cas.ticket.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.logout.LogoutManager;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.util.CasSpringBeanJobFactory;
import org.jasig.cas.web.support.WebUtils;
import org.joda.time.DateTime;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * Implementation of the TicketRegistry that is backed by a ConcurrentHashMap.
 *
 * @author Scott Battaglia
 * @since 3.0.0
 */
@Component("defaultTicketRegistry")
public final class DefaultTicketRegistry extends AbstractTicketRegistry implements Job {

    @Value("${ticket.registry.cleaner.repeatinterval:120}")
    private int refreshInterval;

    @Value("${ticket.registry.cleaner.startdelay:20}")
    private int startDelay;

    @Autowired
    @NotNull
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("logoutManager")
    private LogoutManager logoutManager;

    /**
     * A HashMap to contain the tickets.
     */
    private final Map<String, Ticket> cache;

    /**
     * Instantiates a new default ticket registry.
     */
    public DefaultTicketRegistry() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new, empty registry with the specified initial capacity, load
     * factor, and concurrency level.
     *
     * @param initialCapacity  - the initial capacity. The implementation
     *                         performs internal sizing to accommodate this many elements.
     * @param loadFactor       - the load factor threshold, used to control resizing.
     *                         Resizing may be performed when the average number of elements per bin
     *                         exceeds this threshold.
     * @param concurrencyLevel - the estimated number of concurrently updating
     *                         threads. The implementation performs internal sizing to try to
     *                         accommodate this many threads.
     */
    @Autowired
    public DefaultTicketRegistry(@Value("${default.ticket.registry.initialcapacity:1000}")
                                 final int initialCapacity,
                                 @Value("${default.ticket.registry.loadfactor:1}")
                                 final float loadFactor,
                                 @Value("${default.ticket.registry.concurrency:20}")
                                 final int concurrencyLevel) {
        this.cache = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    /**

     * @throws IllegalArgumentException if the Ticket is null.
     */
    @Override
    public void addTicket(final Ticket ticket) {
        Assert.notNull(ticket, "ticket cannot be null");

        logger.debug("Added ticket [{}] to registry.", ticket.getId());
        this.cache.put(ticket.getId(), ticket);
    }

    @Override
    public Ticket getTicket(final String ticketId) {
        if (ticketId == null) {
            return null;
        }

        logger.debug("Attempting to retrieve ticket [{}]", ticketId);
        final Ticket ticket = this.cache.get(ticketId);

        if (ticket != null) {
            logger.debug("Ticket [{}] found in registry.", ticketId);
        }

        return ticket;
    }

    @Override
    public boolean deleteTicket(final String ticketId) {
        if (ticketId == null) {
            return false;
        }

        final Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            return false;
        }

        if (ticket instanceof TicketGrantingTicket) {
            logger.debug("Removing children of ticket [{}] from the registry.", ticket);
            deleteChildren((TicketGrantingTicket) ticket);
        }

        logger.debug("Removing ticket [{}] from the registry.", ticket);
        return (this.cache.remove(ticketId) != null);
    }
    
    
    /**
     * helper method to remove tickets for cache.
     * 
     * @param ticketId The ticket id.
     * @param cache The cache.
     * @return true is the ticket is deleted. else false.
     */
    public boolean deleteTicket(final String ticketId, final Map<String, Ticket> cache) {
	if (ticketId == null) {
	    return false;
	}
	
	final Ticket ticket = cache.get(ticketId);
	if (ticket == null) {
	    return false;
	}
	
	if (ticket instanceof TicketGrantingTicket) {
	    logger.debug("Removing children of ticket [{}] from the registry.", ticket);
	    deleteChildren((TicketGrantingTicket) ticket);
	}
	
	logger.debug("Removing ticket [{}] from the registry.", ticket);
	return (cache.remove(ticketId) != null);
    }

    /**
     * Delete TGT's service tickets.
     *
     * @param ticket the ticket
     */
    private void deleteChildren(final TicketGrantingTicket ticket) {
        // delete service tickets
        final Map<String, Service> services = ticket.getServices();
        if (services != null && !services.isEmpty()) {
            for (final Map.Entry<String, Service> entry : services.entrySet()) {
                if (this.cache.remove(entry.getKey()) != null) {
                    logger.trace("Removed service ticket [{}]", entry.getKey());
                } else {
                    logger.trace("Unable to remove service ticket [{}]", entry.getKey());
                }
            }
        }
    }

    @Override
    public Collection<Ticket> getTickets() {
        return Collections.unmodifiableCollection(this.cache.values());
    }

    @Override
    public int sessionCount() {
        int count = 0;
        for (final Ticket t : this.cache.values()) {
            if (t instanceof TicketGrantingTicket) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int serviceTicketCount() {
        int count = 0;
        for (final Ticket t : this.cache.values()) {
            if (t instanceof ServiceTicket) {
                count++;
            }
        }
        return count;
    }

    /**
     * Schedule reloader job.
     */
    @PostConstruct
    public void scheduleCleanerJob() {
        try {
            if (shouldScheduleCleanerJob()) {
                logger.info("Preparing to schedule job to clean up after tickets...");

                final JobDetail job = JobBuilder.newJob(this.getClass())
                    .withIdentity(this.getClass().getSimpleName().concat(UUID.randomUUID().toString()))
                    .build();

                final Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(this.getClass().getSimpleName().concat(UUID.randomUUID().toString()))
                    .startAt(DateTime.now().plusSeconds(this.startDelay).toDate())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(this.refreshInterval)
                        .repeatForever()).build();

                final JobFactory jobFactory = new CasSpringBeanJobFactory(this.applicationContext);
                final SchedulerFactory schFactory = new StdSchedulerFactory();
                final Scheduler sch = schFactory.getScheduler();
                sch.getContext().put("ticketsCache", cache);
                sch.setJobFactory(jobFactory);
                sch.start();
                logger.debug("Started {} scheduler", this.getClass().getSimpleName());
                sch.scheduleJob(job, trigger);
                logger.info("{} will clean tickets every {} minutes",
                    this.getClass().getSimpleName(),
                    TimeUnit.SECONDS.toMinutes(this.refreshInterval));
            }
        } catch (final Exception e){
            logger.warn(e.getMessage(), e);
        }

    }

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

        try {
            //get the cache instance from the scheduler context
            Map<String, Ticket> cacheBean = (Map<String, Ticket>) jobExecutionContext.getScheduler().getContext().get("ticketsCache");
            logger.info("total tickets in the registry {} ", cacheBean.size());
            //prepare an unmodifiablecollection.
            final Collection<Ticket> allTickets = Collections.unmodifiableCollection(cacheBean.values());
            logger.info("Beginning ticket cleanup...");
            //filter the expired tickets.
	    final Collection<Ticket> ticketsToRemove = allTickets
                    .parallelStream()
                    .filter(Ticket::isExpired)
                    .collect(Collectors.toSet());
            logger.debug("{} expired tickets found.", ticketsToRemove.size());
            int tgtCount = 0;
            int pgtCount = 0;
            int stCount = 0;
            int ptCount = 0;
            for (final Ticket ticket : ticketsToRemove) {
        	final String tktId = ticket.getId();
                if (ticket instanceof TicketGrantingTicket) {
                    logger.debug("Cleaning up expired ticket-granting ticket [{}]", tktId);
                    logoutManager.performLogout((TicketGrantingTicket) ticket);
                   if( deleteTicket(tktId, cacheBean)){
                       if(tktId.startsWith("TGT-")){
                	   tgtCount++;
                       } else {
                	   pgtCount++;
                       }
                   }
                } else if (ticket instanceof ServiceTicket) {
                    logger.debug("Cleaning up expired service ticket [{}]", ticket.getId());
                    if( deleteTicket(ticket.getId(), cacheBean)){
                	if(tktId.startsWith("ST-")){
                 	   stCount++;
                        } else {
                 	   ptCount++;
                        }
                    }
                } else {
                    logger.warn("Unknown ticket type [{} found to clean", ticket.getClass().getSimpleName());
                }
            }
            logger.trace("ticket removed ServiceTickets: {}, ProxyTickets: {}, TicketGrantingTicket: {}, ProxyGrantingTickets: {} ", stCount, ptCount, tgtCount, pgtCount);
            int totalCount = stCount + ptCount + pgtCount + tgtCount;
            logger.info("{} expired tickets found and removed.", totalCount);
            logger.trace("{} expired tickets found and removed.", totalCount);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private boolean shouldScheduleCleanerJob() {
        if (this.startDelay > 0 && this.applicationContext.getParent() == null) {
            if (WebUtils.isCasServletInitializing(this.applicationContext)) {
                logger.debug("Found CAS servlet application context");
                final String[] aliases =
                    this.applicationContext.getAutowireCapableBeanFactory().getAliases("defaultTicketRegistry");

                if (aliases.length > 0) {
                    logger.debug("{} is used as the active current ticket registry", this.getClass().getSimpleName());
                    return true;
                }
                return false;
            }
        }

        return false;
    }
}
