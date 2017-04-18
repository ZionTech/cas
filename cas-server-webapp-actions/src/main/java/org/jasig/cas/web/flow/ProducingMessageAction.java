package org.jasig.cas.web.flow;

import java.util.Calendar;
import java.util.EnumMap;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.jasig.cas.authentication.Credential;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.web.support.WebUtils;
import org.jasig.cas.web.wavity.event.EventPublisher;
import org.jasig.cas.web.wavity.event.EventResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.wavity.broker.util.EventType;

/**
 * Performs the message producing for CAS login events.
 *
 * @author davidlee
 * 
 */
@Component("producingMessageAction")
public class ProducingMessageAction extends AbstractAction {

    @NotNull
    private final ServicesManager servicesManager;

    /**
     * Initialize the component with an instance of the services manager.
     * @param servicesManager the service registry instance.
     */
    @Autowired
    public ProducingMessageAction(@Qualifier("servicesManager") final ServicesManager servicesManager) {
        this.servicesManager = servicesManager;
    }

    @Override
    protected Event doExecute(final RequestContext context) throws Exception {
		final HttpServletRequest request = WebUtils.getHttpServletRequest(context);

		String tenantId = null;

		if (request != null) {
			tenantId = AuthUtils.extractTenantID(request);
		}

		final Credential credential = WebUtils.getCredential(context);

		if ((tenantId == null || "".equals(tenantId)) || credential == null) {
			logger.error("*** Tenant ID can't be empty or null, or credential is not valid ***");
			return error();
		}

		final String message = String.format("The user %s logged in successfully", credential.toString());
		try {
			EventPublisher.publishEvent(context.getMessageContext(),
					EventType.EVENT_TYPE_SSO_AUTHENTICATION, tenantId, EventResult.SUCCESS, message);
			logger.info("Successfully published login event for a user " + credential.toString());
		} catch (final Exception e) {
			logger.warn("Could not publish login event for a user "+ credential.toString(), e);
		}

		return success();
    }
}
