package org.jasig.cas.web.flow;

import java.util.Collection;

import javax.validation.constraints.NotNull;
import javax.ws.rs.NotAuthorizedException;

import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Predicates;

/**
 * Process the /users URL requests.
 * <p>
 * Obtain the userId and loggedInUserId. Get all the ticket from central
 * authentication server, check the access permission for logged in user from
 * pricipal object. Validate the ticket from authentication pricncipal and
 * delete all active TGT,PGT and child tickets from ticket registry.
 *
 * @author Venkatesh T
 */

@RestController("deleteUserSessionController")
public class DeleteUserSessionController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUserSessionController.class);

	/**
	 * * Tenant administration DN.
	 */
	private final String TENANT_ADMIN_DN_FORMAT = "cn=admin,ou=Tenant Administration,ou=Privileges,cn=%s,o=Tenants,dc=wavity,dc=com";

	/**
	 * Buyer DN.
	 */
	private final String BUYER_DN_FORMAT = "cn=buyer,ou=Buyer,ou=Privileges,cn=%s,o=Tenants,dc=wavity,dc=com";

	private final String isMemberOf = "isMemberOf";

	private final String cn = "cn";

	/** Core we delegate to for handling all ticket related tasks. */
	@NotNull
	@Autowired
	@Qualifier("centralAuthenticationService")
	private CentralAuthenticationService centralAuthenticationService;

	@RequestMapping(value = "/invalidateUserSession", method = RequestMethod.POST)
	public final ResponseEntity<String> deleteUserSession(@RequestParam("deletedUserId") final String userId,
			@RequestParam("loggedInUserId") final String loggedInUserId) {
		final Collection<Ticket> tickets = centralAuthenticationService.getTickets(Predicates.<Ticket>alwaysTrue());
		LOGGER.debug("Tickets count {}", (tickets == null) ? 0 : tickets.size());
		try {
			if (checkAcess(tickets, loggedInUserId)) {
				for (final Ticket ticket : tickets) {
					if (ticket instanceof TicketGrantingTicket && !ticket.isExpired()) {
						final TicketGrantingTicket tgticket = (TicketGrantingTicket) ticket;
						final Principal principal = getPricipal(tgticket);
						if (principal != null && principal.getId().equalsIgnoreCase(userId)) {
							LOGGER.debug("Destory the TGT or PGT for deleted user {}, ticketId {}", userId,
									tgticket.getId());
							centralAuthenticationService.destroyTicketGrantingTicket(tgticket.getId());
						}
					}
				}
			} else {
				LOGGER.error(
						"Check access failed for logged in user. Logged in user should be a buyer or tenant admin");
				throw new NotAuthorizedException("Unauthorized: Check access failed for logged in user");
			}
		} catch (final NotAuthorizedException e) {
			LOGGER.error("Exception occured While checking access for logged in user {} .", loggedInUserId);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
		} catch (final Exception e) {
			LOGGER.error("Exception occured While deleting user session and its attributes for deleted user {} .",
					userId);
			return new ResponseEntity<>("Exception occured While deleting user session ",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Check access for the logged in user can access for delete session or not
	 *
	 * @param tickets
	 * @param loggedInUserId
	 * @return access check boolean
	 */
	private boolean checkAcess(final Collection<Ticket> tickets, final String loggedInUserId)
			throws NotAuthorizedException {
		LOGGER.debug("Check access for logged in for destroy the session");
		for (final Ticket ticket : tickets) {
			if (ticket instanceof TicketGrantingTicket && !ticket.isExpired()) {
				final TicketGrantingTicket tgticket = (TicketGrantingTicket) ticket;
				LOGGER.debug("Processing for check access to the user {}", tgticket.getId());
				// Here is we are getting both TGT & PGT tickets so i add a TGT
				// condition.
				if (tgticket.getId().startsWith("TGT")) {
					final Principal principal = getPricipal(tgticket);
					if (principal.getId().equalsIgnoreCase(loggedInUserId)) {
						if (principal.getAttributes().containsKey(isMemberOf)
								&& principal.getAttributes().containsKey(cn)) {
							final String member = (String) principal.getAttributes().get(isMemberOf);
							final String tenant = (String) principal.getAttributes().get(cn);
							if (member.contains(String.format(BUYER_DN_FORMAT, tenant))
									|| member.contains(String.format(TENANT_ADMIN_DN_FORMAT, tenant))) {
								LOGGER.debug(
										"Logged in user has the access for the delete the user session loggedin user {}",
										loggedInUserId);
								return true;
							} else {
								LOGGER.debug("Logged in user not a Buyer or Tenant admin");// not
																							// auto
								throw new NotAuthorizedException(
										"Unauthorized: Logged in user not a Buyer or Tenant admin");
							}
						} else {
							LOGGER.debug("Given TGT does not contain isMemeber of attribute :: TGT {}", tgticket);
							throw new NotAuthorizedException(
									"Unauthorized: Given TGT does not contain isMemeber of attribute");
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Get the principal object from TGT and PGT
	 *
	 * @param tgticket
	 * @param ticket
	 * @return Principal
	 */
	private Principal getPricipal(final TicketGrantingTicket tgticket) {
		Principal principal = null;
		if (tgticket.getId() != null && tgticket.getId().startsWith("TGT")) {
			principal = tgticket.getAuthentication().getPrincipal();
		} else if (tgticket.getId() != null && tgticket.getId().startsWith("PGT")) {
			if (tgticket.getGrantingTicket().getId().startsWith("TGT"))
				principal = tgticket.getGrantingTicket().getAuthentication().getPrincipal();
			if (tgticket.getGrantingTicket().getId().startsWith("PGT"))
				principal = tgticket.getGrantingTicket().getGrantingTicket().getAuthentication().getPrincipal();
		}
		return principal;
	}

	public void setCentralAuthenticationService(final CentralAuthenticationService centralAuthenticationService) {
		this.centralAuthenticationService = centralAuthenticationService;
	}

}
