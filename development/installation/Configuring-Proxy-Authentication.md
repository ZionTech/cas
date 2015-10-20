---
layout: default
title: CAS - Proxy Authentication
---

# Proxy Authentication

Proxy authentication support for CAS v1+ protocols is enabled by default, thus it is entirely a matter of CAS
client configuration to leverage proxy authentication features.

<div class="alert alert-info"><strong>Service Configuration</strong><p>
Note that each registered application in the registry must explicitly be configured
to allow for proxy authentication. See <a href="Service-Management.html">this guide</a>
to learn about registering services in the registry.
</p></div>

Disabling proxy authentication components is recommended for deployments that wish to strategically avoid proxy
authentication as a matter of security policy.

##Handling SSL-enabled Proxy URLs
By default, CAS ships with a bundled HTTP client that is partly responsible to callback the URL
for proxy authentication. Note that this URL need also be authorized by the CAS service registry
before the callback can be made. [See this guide](Service-Management.md) for more info.

If the callback URL is authorized by the service registry, and if the endpoint is under HTTPS
and protected by an SSL certificate, CAS will also attempt to verify the validity of the endpoint's
certificate before it can establish a successful connection. If the certificate is invalid, expired,
missing a step in its chain, self-signed or otherwise, CAS will fail to execute the callback.

The HTTP client of CAS does present a local trust store that is similar to that of the Java platform.
It is recommended that this trust store be used to handle the management of all certificates that need
to be imported into the platform to allow CAS to execute the callback URL successfully. While by default,
the local trust store to CAS is empty, CAS will still utilize **both** the default and the local trust store.
The local trust store should only be used for CAS-related functionality of course, and the trust store file
can be carried over across CAS and Java upgrades, and certainly managed by the source control system that should
host all CAS configuration.

{% highlight xml %}
# The http client truststore file, in addition to the default's
# http.client.truststore.file=classpath:truststore.jks
#
# The http client truststore's password
# http.client.truststore.psw=changeit
{% endhighlight %}

##Returning PGT in Validation Response
In situations where using `CAS20ProxyHandler` may be undesirable, such that invoking a callback url to receive the proxy granting ticket is not feasible,
CAS may be configured to return the proxy-granting ticket id directly in the validation response. In order to successfully establish trust between the
CAS server and the application, private/public key pairs are generated by the client application and then **the public key** distributed and
configured inside CAS. CAS will use the public key to encrypt the proxy granting ticket id and will issue a new attribute `<proxyGrantingTicketId>`
in the validation response, only if the service is authorized to receive it.

Note that the return of the proxy granting ticket id is only carried out by the CAS validation response, provided the client
application issues a request to the `/p3/serviceValidate` endpoint (or `/p3/proxyValidate`). Other means of returning attributes to CAS, such as SAML1
will **not** support the additional returning of the proxy granting ticket.

###Configuration

####Register the public key for service
Once you have received the public key from the client application owner, it must be first registered inside the CAS server's service registry:

{% highlight xml %}
...
<property name="publicKey">
    <bean class="org.jasig.cas.services.RegisteredServicePublicKeyImpl"
          c:location="classpath:RSA1024Public.key"
          c:algorithm="RSA" />
</property>
...
{% endhighlight %}

####Authorize PGT for Service
The service that holds the public key above must also be authorized to receive the proxy granting ticket id
as an attribute for the given attribute release policy of choice:

{% highlight xml %}
...
<property name="attributeReleasePolicy">
    <bean class="org.jasig.cas.services.ReturnAllowedAttributeReleasePolicy"
            p:authorizedToReleaseProxyGrantingTicket="true" />
</property>
...
{% endhighlight %}

####Decrypt the PGT id
Once the client application has received the `proxyGrantingTicket` id attribute in the CAS validation response, it can decrypt it
via its own private key. Since the attribute is base64 encoded by default, it needs to be decoded first before
decryption can occur. Here's a sample code snippet:

{% highlight java %}

final Map<?, ?> attributes = ...
final String encodedPgt = (String) attributes.get("proxyGrantingTicket");
final PrivateKey privateKey = ...
final Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
final byte[] cred64 = decodeBase64ToByteArray(encodedPgt);
cipher.init(Cipher.DECRYPT_MODE, privateKey);
final byte[] cipherData = cipher.doFinal(cred64);
return new String(cipherData);

{% endhighlight %}