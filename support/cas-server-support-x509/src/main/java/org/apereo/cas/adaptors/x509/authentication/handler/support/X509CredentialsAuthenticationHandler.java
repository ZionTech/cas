package org.apereo.cas.adaptors.x509.authentication.handler.support;

import org.apereo.cas.adaptors.x509.authentication.principal.X509CertificateCredential;
import org.apereo.cas.adaptors.x509.util.CertUtils;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.DefaultHandlerResult;
import org.apereo.cas.authentication.HandlerResult;
import org.apereo.cas.authentication.PreventedException;
import org.apereo.cas.authentication.handler.support.AbstractPreAndPostProcessingAuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.security.auth.login.FailedLoginException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Authentication Handler that accepts X509 Certificates, determines their
 * validity and ensures that they were issued by a trusted issuer. (targeted at
 * X509v3) Optionally checks KeyUsage extension in the user certificate
 * (container should do that too). Note that this handler trusts the servlet
 * container to do some initial checks like path validation. Deployers can
 * supply an optional pattern to match subject dns against to further restrict
 * certificates in case they are not using their own issuer. It's also possible
 * to specify a maximum pathLength for the SUPPLIED certificates. (note that
 * this does not include a pathLength check for the root certificate)
 * [PathLength is 0 for the CA certificate that issues the end-user certificate]
 *
 * @author Scott Battaglia
 * @author Jan Van der Velpen
 * @since 3.0.4
 */
public class X509CredentialsAuthenticationHandler extends AbstractPreAndPostProcessingAuthenticationHandler {

    /**
     * OID for KeyUsage X.509v3 extension field.
     */
    private static final String KEY_USAGE_OID = "2.5.29.15";

    /**
     * Instance of Logging.
     */
    private transient Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The compiled pattern supplied by the deployer.
     */
    private Pattern regExTrustedIssuerDnPattern;

    /**
     * Deployer supplied setting for maximum pathLength in a SUPPLIED
     * certificate.
     */
    private int maxPathLength;

    /**
     * Deployer supplied setting to allow unlimited pathLength in a SUPPLIED
     * certificate.
     */
    private boolean maxPathLengthAllowUnspecified;

    /**
     * Deployer supplied setting to check the KeyUsage extension.
     */
    private boolean checkKeyUsage;

    /**
     * Deployer supplied setting to force require the correct KeyUsage
     * extension.
     */
    private boolean requireKeyUsage;

    /**
     * The compiled pattern for trusted DN's supplied by the deployer.
     */
    private Pattern regExSubjectDnPattern;

    /**
     * Certificate revocation checker component.
     */

    private RevocationChecker revocationChecker = new NoOpRevocationChecker();


    @Override
    public boolean supports(final Credential credential) {
        return credential != null
                && X509CertificateCredential.class.isAssignableFrom(credential
                .getClass());
    }

    /**
     * Init and ensure configuration is correct.
     */
    @PostConstruct
    public void init() {
        if (this.regExSubjectDnPattern == null) {
            throw new IllegalArgumentException("Subject DN pattern is not configured");
        }
        if (this.revocationChecker == null) {
            throw new IllegalArgumentException("Revocation checker is not configured");
        }
        if (this.regExTrustedIssuerDnPattern == null) {
            throw new IllegalArgumentException("Trusted issuer DN pattern is not configured");
        }
    }

    @Override
    protected HandlerResult doAuthentication(final Credential credential) throws GeneralSecurityException, PreventedException {

        final X509CertificateCredential x509Credential = (X509CertificateCredential) credential;
        final X509Certificate[] certificates = x509Credential.getCertificates();

        X509Certificate clientCert = null;
        boolean hasTrustedIssuer = false;
        for (int i = certificates.length - 1; i >= 0; i--) {
            final X509Certificate certificate = certificates[i];
            logger.debug("Evaluating {}", CertUtils.toString(certificate));

            validate(certificate);

            if (!hasTrustedIssuer) {
                hasTrustedIssuer = isCertificateFromTrustedIssuer(certificate);
            }

            // getBasicConstraints returns pathLenContraint which is generally
            // >=0 when this is a CA cert and -1 when it's not
            final int pathLength = certificate.getBasicConstraints();
            if (pathLength < 0) {
                logger.debug("Found valid client certificate");
                clientCert = certificate;
            } else {
                logger.debug("Found valid CA certificate");
            }
        }
        if (hasTrustedIssuer && clientCert != null) {
            x509Credential.setCertificate(clientCert);
            return new DefaultHandlerResult(this, x509Credential, this.principalFactory.createPrincipal(x509Credential.getId()));
        }
        logger.warn("Either client certificate could not be determined, or a trusted issuer could not be located");
        throw new FailedLoginException();
    }

    public void setTrustedIssuerDnPattern(final String trustedIssuerDnPattern) {
        this.regExTrustedIssuerDnPattern = Pattern.compile(trustedIssuerDnPattern);
    }

    /**
     * @param maxPathLength The maxPathLength to set.
     */
    public void setMaxPathLength(
            final int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    /**
     * @param allowed Allow CA certs to have unlimited intermediate certs (default=false).
     */

    public void setMaxPathLengthAllowUnspecified(
            final boolean allowed) {
        this.maxPathLengthAllowUnspecified = allowed;
    }

    /**
     * @param checkKeyUsage The checkKeyUsage to set.
     */
    public void setCheckKeyUsage(
            final boolean checkKeyUsage) {
        this.checkKeyUsage = checkKeyUsage;
    }

    /**
     * @param requireKeyUsage The requireKeyUsage to set.
     */
    public void setRequireKeyUsage(
            final boolean requireKeyUsage) {
        this.requireKeyUsage = requireKeyUsage;
    }


    public void setSubjectDnPattern(
            final String subjectDnPattern) {
        this.regExSubjectDnPattern = Pattern.compile(subjectDnPattern);
    }

    /**
     * Sets the component responsible for evaluating certificate revocation status for client
     * certificates presented to handler. The default checker is a NO-OP implementation
     * for backward compatibility with previous versions that do not perform revocation
     * checking.
     *
     * @param checker Revocation checker component.
     */
    public void setRevocationChecker(final RevocationChecker checker) {
        this.revocationChecker = checker;
    }

    /**
     * Validate the X509Certificate received.
     *
     * @param cert the cert
     * @throws GeneralSecurityException the general security exception
     */
    private void validate(final X509Certificate cert) throws GeneralSecurityException {
        cert.checkValidity();
        this.revocationChecker.check(cert);

        final int pathLength = cert.getBasicConstraints();
        if (pathLength < 0) {
            if (!isCertificateAllowed(cert)) {
                throw new FailedLoginException(
                        "Certificate subject does not match pattern " + this.regExSubjectDnPattern.pattern());
            }
            if (this.checkKeyUsage && !isValidKeyUsage(cert)) {
                throw new FailedLoginException(
                        "Certificate keyUsage constraint forbids SSL client authentication.");
            }
        } else {
            // Check pathLength for CA cert
            if (pathLength == Integer.MAX_VALUE && !this.maxPathLengthAllowUnspecified) {
                throw new FailedLoginException("Unlimited certificate path length not allowed by configuration.");
            } else if (pathLength > this.maxPathLength && pathLength < Integer.MAX_VALUE) {
                throw new FailedLoginException(String.format(
                        "Certificate path length %s exceeds maximum value %s.", pathLength, this.maxPathLength));
            }
        }
    }

    /**
     * Checks if is valid key usage. <p>
     * KeyUsage ::= BIT STRING { digitalSignature (0), nonRepudiation (1),
     * keyEncipherment (2), dataEncipherment (3), keyAgreement (4),
     * keyCertSign (5), cRLSign (6), encipherOnly (7), decipherOnly (8) }
     *
     * @param certificate the certificate
     * @return true, if  valid key usage
     */
    private boolean isValidKeyUsage(final X509Certificate certificate) {
        logger.debug("Checking certificate keyUsage extension");
        final boolean[] keyUsage = certificate.getKeyUsage();
        if (keyUsage == null) {
            logger.warn("Configuration specifies checkKeyUsage but keyUsage extension not found in certificate.");
            return !this.requireKeyUsage;
        }

        final boolean valid;
        if (isCritical(certificate, KEY_USAGE_OID) || this.requireKeyUsage) {
            logger.debug("KeyUsage extension is marked critical or required by configuration.");
            valid = keyUsage[0];
        } else {
            logger.debug(
                    "KeyUsage digitalSignature=%s, Returning true since keyUsage validation not required by configuration.");
            valid = true;
        }
        return valid;
    }

    /**
     * Checks if critical extension oids contain the extension oid.
     *
     * @param certificate  the certificate
     * @param extensionOid the extension oid
     * @return true, if  critical
     */
    private static boolean isCritical(final X509Certificate certificate, final String extensionOid) {
        final Set<String> criticalOids = certificate.getCriticalExtensionOIDs();

        if (criticalOids == null || criticalOids.isEmpty()) {
            return false;
        }

        return criticalOids.contains(extensionOid);
    }

    /**
     * Checks if is certificate allowed based no the pattern given.
     *
     * @param cert the cert
     * @return true, if  certificate allowed
     */
    private boolean isCertificateAllowed(final X509Certificate cert) {
        return doesNameMatchPattern(cert.getSubjectDN(), this.regExSubjectDnPattern);
    }

    /**
     * Checks if is certificate from trusted issuer based on the regex pattern.
     *
     * @param cert the cert
     * @return true, if  certificate from trusted issuer
     */
    private boolean isCertificateFromTrustedIssuer(final X509Certificate cert) {
        return doesNameMatchPattern(cert.getIssuerDN(), this.regExTrustedIssuerDnPattern);
    }

    /**
     * Does principal name match pattern?
     *
     * @param principal the principal
     * @param pattern   the pattern
     * @return true, if successful
     */
    private boolean doesNameMatchPattern(final Principal principal,
                                         final Pattern pattern) {

        if (pattern != null) {
            final String name = principal.getName();
            final boolean result = pattern.matcher(name).matches();
            logger.debug(String.format("%s matches %s == %s", pattern.pattern(), name, result));
            return result;
        }
        return true;
    }
}