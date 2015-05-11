package org.osiam.addons.self_administration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Holds all application wide used properties and enables scheduling
 */
@Configuration
public class Config {

    @Value("${org.osiam.scim.extension.urn}")
    private String extensionUrn;

    @Value("${org.osiam.scim.extension.field.activationtoken}")
    private String activationTokenField;

    @Value("${org.osiam.scim.extension.field.onetimepassword}")
    private String oneTimePasswordField;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.registration.activation-token-timeout:24h}\").getMillis()}")
    private long activationTokenTimeout;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.lost-password.one-time-password-timeout:24h}\").getMillis()}")
    private long oneTimePasswordTimeout;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.change-email.confirmation-token-timeout:24h}\").getMillis()}")
    private long confirmationTokenTimeout;

    @Value("${org.osiam.scim.extension.field.tempemail}")
    private String tempEmailField;

    @Value("${org.osiam.scim.extension.field.emailconfirmtoken}")
    private String confirmationTokenField;

    @Value("${org.osiam.mail.from}")
    private String fromAddress;

    // css and js libs
    @Value("${org.osiam.html.dependencies.bootstrap}")
    private String bootStrapLib;

    @Value("${org.osiam.html.dependencies.angular}")
    private String angularLib;

    @Value("${org.osiam.html.dependencies.jquery}")
    private String jqueryLib;

    @Value("${org.osiam.addon-self-administration.one-time-token-scavenger.enabled:true}")
    private boolean oneTimeTokenScavengerEnabled;

    public String getFromAddress() {
        return fromAddress;
    }

    public String getBootStrapLib() {
        return bootStrapLib;
    }

    public String getAngularLib() {
        return angularLib;
    }

    public String getJqueryLib() {
        return jqueryLib;
    }

    public String getExtensionUrn() {
        return extensionUrn;
    }

    public String getActivationTokenField() {
        return activationTokenField;
    }

    public String getConfirmationTokenField() {
        return confirmationTokenField;
    }

    public String getTempEmailField() {
        return tempEmailField;
    }

    public String getOneTimePasswordField() {
        return oneTimePasswordField;
    }

    public long getActivationTokenTimeout() {
        return activationTokenTimeout;
    }

    public long getConfirmationTokenTimeout() {
        return confirmationTokenTimeout;
    }

    public long getOneTimePasswordTimeout() {
        return oneTimePasswordTimeout;
    }

    public boolean isOneTimeTokenScavengerEnabled() {
        return oneTimeTokenScavengerEnabled;
    }
}
