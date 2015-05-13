package org.osiam.addons.self_administration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds all application wide used properties
 */
@Component
public class Config {

    public static final String EXTENSION_URN = "urn:org.osiam:scim:extensions:addon-self-administration";

    public static final String ACTIVATION_TOKEN_FIELD = "activationToken";

    public static final String ONETIME_PASSWORD_FIELD = "oneTimePassword";

    public static final String TEMP_EMAIL_FIELD = "tempMail";

    public static final String CONFIRMATION_TOKEN_FIELD = "emailConfirmToken";

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.registration.activation-token-timeout:24h}\").getMillis()}")
    private long activationTokenTimeout;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.lost-password.one-time-password-timeout:24h}\").getMillis()}")
    private long oneTimePasswordTimeout;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.change-email.confirmation-token-timeout:24h}\").getMillis()}")
    private long confirmationTokenTimeout;

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

    @Value("${org.osiam.mail.emailchange.linkprefix}")
    private String emailChangeLinkPrefix;

    /* URI for the change email call from JavaScript */
    @Value("${org.osiam.html.emailchange.url}")
    private String clientEmailChangeUri;

    /* Password lost email configuration */
    @Value("${org.osiam.mail.passwordlost.linkprefix}")
    private String passwordLostLinkPrefix;

    /* URI for the change password call from JavaScript */
    @Value("${org.osiam.html.passwordlost.url}")
    private String clientPasswordChangeUri;

    private String[] allAllowedFields;

    @Value("${org.osiam.html.form.usernameEqualsEmail:true}")
    private boolean usernameEqualsEmail;

    @Value("${org.osiam.html.form.password.length:8}")
    private int passwordLength;

    private boolean confirmPasswordRequired = true;

    @Value("${org.osiam.resource-server.home}")
    private String resourceServerHome;

    @Value("${org.osiam.auth-server.home}")
    private String authServerHome;

    @Value("${org.osiam.addon-self-administration.client.id}")
    private String clientId;

    @Value("${org.osiam.addon-self-administration.client.secret}")
    private String clientSecret;

    @Autowired
    private void createAllAllowedFields(@Value("${org.osiam.html.form.fields:}") String[] allowedFields,
            @Value("${org.osiam.html.form.extensions:}") String[] extensions) {
        List<String> collectedFields = new ArrayList<>();

        for (String field : allowedFields) {
            collectedFields.add(field.trim());
        }

        if (!collectedFields.contains("email")) {
            collectedFields.add("email");
        }

        if (!collectedFields.contains("password")) {
            collectedFields.add("password");
        }

        if (!collectedFields.contains("confirmPassword")) {
            confirmPasswordRequired = false;
        }

        String fieldUserName = "userName";
        if (!usernameEqualsEmail && !collectedFields.contains(fieldUserName)) {
            collectedFields.add(fieldUserName);
        } else if (usernameEqualsEmail && collectedFields.contains(fieldUserName)) {
            collectedFields.remove(fieldUserName);
        }

        for (String field : extensions) {
            collectedFields.add(field.trim());
        }

        this.allAllowedFields = collectedFields.toArray(new String[collectedFields.size()]);
    }

    public String getResourceServerHome() {
        return resourceServerHome;
    }

    public String getAuthServerHome() {
        return authServerHome;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String[] getAllAllowedFields() {
        return allAllowedFields.clone();
    }

    public int getPasswordLength() {
        return passwordLength;
    }

    public boolean isUsernameEqualsEmail() {
        return usernameEqualsEmail;
    }

    public boolean isConfirmPasswordRequired() {
        return confirmPasswordRequired;
    }

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

    public String getEmailChangeLinkPrefix() {
        return emailChangeLinkPrefix;
    }

    public String getClientEmailChangeUri() {
        return clientEmailChangeUri;
    }

    public String getPasswordLostLinkPrefix() {
        return passwordLostLinkPrefix;
    }

    public String getClientPasswordChangeUri() {
        return clientPasswordChangeUri;
    }
}
