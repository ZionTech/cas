package org.jasig.cas.authentication.principal;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jasig.cas.logout.SingleLogoutService;
import org.jasig.cas.validation.ValidationResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract implementation of a WebApplicationService.
 *
 * @author Scott Battaglia
 * @since 3.1
 */
public abstract class AbstractWebApplicationService implements SingleLogoutService {

    private static final long serialVersionUID = 610105280927740076L;

    private static final Map<String, Object> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<String, Object>());

    /** Logger instance. **/
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The id of the service. */
    private final String id;

    /** The original url provided, used to reconstruct the redirect url. */
    private final String originalUrl;

    private final String artifactId;

    private Principal principal;

    private boolean loggedOutAlready;

    private final ResponseBuilder<WebApplicationService> responseBuilder;

    private ValidationResponseType format = ValidationResponseType.XML;

    /**
     * Instantiates a new abstract web application service.
     *
     * @param id the id
     * @param originalUrl the original url
     * @param artifactId the artifact id
     * @param responseBuilder the response builder
     */
    protected AbstractWebApplicationService(final String id, final String originalUrl,
            final String artifactId, final ResponseBuilder<WebApplicationService> responseBuilder) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.artifactId = artifactId;
        this.responseBuilder = responseBuilder;
    }

    @Override
    public final String toString() {
        return this.id;
    }

    @Override
    public final String getId() {
        return this.id;
    }

    @Override
    public final String getArtifactId() {
        return this.artifactId;
    }

    @Override
    public final Map<String, Object> getAttributes() {
        return EMPTY_MAP;
    }

    /**
     * Return the original url provided (as {@code service} or {@code targetService} request parameter).
     * Used to reconstruct the redirect url.
     *
     * @return the original url provided.
     */
    @Override
    public final String getOriginalUrl() {
        return this.originalUrl;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null) {
            return false;
        }

        if (object instanceof Service) {
            final Service service = (Service) object;

            return getId().equals(service.getId());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.id)
                .toHashCode();
    }

    public Principal getPrincipal() {
        return this.principal;
    }

    @Override
    public void setPrincipal(final Principal principal) {
        this.principal = principal;
    }

    @Override
    public boolean matches(final Service service) {
    	boolean matched=false;
        try {
            final String thisUrl = URLDecoder.decode(this.id, "UTF-8");
            final String serviceUrl = URLDecoder.decode(service.getId(), "UTF-8");

            logger.trace("Decoded urls and comparing [{}] with [{}]", thisUrl, serviceUrl);
            matched = thisUrl.equalsIgnoreCase(serviceUrl);
            /**
             * Checking to see whether difference between serviceUrl and thisUrl is just differences in 
             * highestLevelDomain and port number. If thisUrl contains .local we ignore the port number and
             * highestLevelDomain.
             */
            
            if(!matched && thisUrl.contains(".local".intern())){
            	URL thisURL = new URL(thisUrl);
        		URL serviceURL = new URL(serviceUrl);
        		if(thisURL.getHost().endsWith(".local")){
        			String hostName = thisURL.getHost().substring(0, thisURL.getHost().lastIndexOf(".local")-1);
        			String serviceURLHost = serviceURL.getHost();
        			if(serviceURLHost.startsWith(hostName)){
        				matched = ( thisURL.getPath().equalsIgnoreCase(serviceURL.getPath()) &&
        						thisURL.getProtocol().equalsIgnoreCase(serviceURL.getProtocol()));
        			}
        		}
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
        return matched;
    }

    /**
     * Return if the service is already logged out.
     *
     * @return if the service is already logged out.
     */
    @Override
    public boolean isLoggedOutAlready() {
        return loggedOutAlready;
    }

    /**
     * Set if the service is already logged out.
     *
     * @param loggedOutAlready if the service is already logged out.
     */
    @Override
    public final void setLoggedOutAlready(final boolean loggedOutAlready) {
        this.loggedOutAlready = loggedOutAlready;
    }

    protected ResponseBuilder<? extends WebApplicationService> getResponseBuilder() {
        return responseBuilder;
    }

    @Override
    public ValidationResponseType getFormat() {
        return format;
    }

    public void setFormat(final ValidationResponseType format) {
        this.format = format;
    }

    @Override
    public Response getResponse(final String ticketId) {
        return this.responseBuilder.build(this, ticketId);

    }
}
