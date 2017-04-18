package org.jasig.cas.web.flow;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.AuthenticationSystemSupport;
import org.jasig.cas.authentication.DefaultAuthenticationSystemSupport;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.logout.LogoutRequest;
import org.jasig.cas.ticket.InvalidTicketException;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.web.support.CookieRetrievingCookieGenerator;
import org.jasig.cas.web.support.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.webflow.action.EventFactorySupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Terminates the CAS SSO session by destroying all SSO state data (i.e. TGT, cookies).
 *
 * @author Marvin S. Addison
 * @since 4.0.0
 */
@Component("terminateSessionAction")
public final class TerminateSessionAction {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
    /** Webflow event helper component. */
    private final EventFactorySupport eventFactorySupport = new EventFactorySupport();

    /** The CORE to which we delegate for all CAS functionality. */
    @NotNull
    @Autowired
    @Qualifier("centralAuthenticationService")
    private CentralAuthenticationService centralAuthenticationService;

    /** CookieGenerator for TGT Cookie. */
    @NotNull
    @Autowired
    @Qualifier("ticketGrantingTicketCookieGenerator")
    private CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;

    /** CookieGenerator for Warn Cookie. */
    @NotNull
    @Autowired
    @Qualifier("warnCookieGenerator")
    private CookieRetrievingCookieGenerator warnCookieGenerator;

    @NotNull
    @Autowired(required=false)
    @Qualifier("defaultAuthenticationSystemSupport")
    private AuthenticationSystemSupport authenticationSystemSupport = new DefaultAuthenticationSystemSupport();

    /**
     * Creates a new instance with the given parameters.
     */
    public TerminateSessionAction() {}

    /**
     * Terminates the CAS SSO session by destroying the TGT (if any) and removing cookies related to the SSO session.
     *
     * @param context Request context.
     *
     * @return "success"
     */
    public Event terminate(final RequestContext context) {
        // in login's webflow : we can get the value from context as it has already been stored
        String tgtId = WebUtils.getTicketGrantingTicketId(context);
        // for logout, we need to get the cookie's value
        if (tgtId == null) {
            final HttpServletRequest request = WebUtils.getHttpServletRequest(context);
            tgtId = this.ticketGrantingTicketCookieGenerator.retrieveCookieValue(request);
        }
        if (tgtId != null) {
        	final String flowDefinitionId = context.getFlowExecutionContext().getDefinition().getId();
        	if(flowDefinitionId != null && flowDefinitionId.equals("logout")) {
        		try {
        			final TicketGrantingTicket ticket = this.centralAuthenticationService.getTicket(tgtId, TicketGrantingTicket.class);
        			WebUtils.addValueInMessageContext(context.getMessageContext(), "tgtIsNull", Boolean.toString(false));
        			WebUtils.addPrincipalInMessageContext(context, ticket);
        		} catch (InvalidTicketException e) {
        			logger.error("Error to get a tgt : " + e.toString());
        		}
        	}
        	
            final List<LogoutRequest> logoutRequests = this.centralAuthenticationService.destroyTicketGrantingTicket(tgtId);
            WebUtils.putLogoutRequests(context, logoutRequests);
        }
        final HttpServletResponse response = WebUtils.getHttpServletResponse(context);
        this.ticketGrantingTicketCookieGenerator.removeCookie(response);
        this.warnCookieGenerator.removeCookie(response);
        return this.eventFactorySupport.success(this);
    }
}
