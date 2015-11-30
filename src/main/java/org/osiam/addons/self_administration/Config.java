package org.osiam.addons.self_administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all application wide used properties
 */
@Configuration
public class Config extends WebMvcConfigurerAdapter {

    public static final String EXTENSION_URN = "urn:org.osiam:scim:extensions:addon-self-administration";

    public static final String ACTIVATION_TOKEN_FIELD = "activationToken";

    public static final String ONETIME_PASSWORD_FIELD = "oneTimePassword";

    public static final String TEMP_EMAIL_FIELD = "tempMail";

    public static final String CONFIRMATION_TOKEN_FIELD = "emailConfirmToken";

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

    @Value("${org.osiam.addon-self-administration.client.secret}")
    private String clientSecret;

    @Value("${org.osiam.addon-self-administration.client.id:addon-self-administration-client}")
    private String clientId;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.registration.activation-token-timeout:24h}\").getMillis()}")
    private long activationTokenTimeout;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.lost-password.one-time-password-timeout:24h}\").getMillis()}")
    private long oneTimePasswordTimeout;

    @Value("#{T(org.osiam.addons.self_administration.util.SelfAdministrationHelper).makeDuration(" +
            "\"${org.osiam.addon-self-administration.change-email.confirmation-token-timeout:24h}\").getMillis()}")
    private long confirmationTokenTimeout;

    // css and js libs
    @Value("${org.osiam.html.dependencies.bootstrap:http://getbootstrap.com/dist/css/bootstrap.css}")
    private String bootStrapLib;

    @Value("${org.osiam.html.dependencies.angular:http://code.angularjs.org/1.2.0-rc.3/angular.min.js}")
    private String angularLib;

    @Value("${org.osiam.html.dependencies.jquery:http://ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js}")
    private String jqueryLib;

    @Value("${org.osiam.addon-self-administration.one-time-token-scavenger.enabled:true}")
    private boolean oneTimeTokenScavengerEnabled;

    @Value("${org.osiam.html.form.usernameEqualsEmail:true}")
    private boolean usernameEqualsEmail;

    @Value("${org.osiam.html.form.password.length:8}")
    private int passwordLength;

    @Value("${org.osiam.home:}")
    private String osiamHome;

    @Value("${org.osiam.resource-server.home:http://localhost:8080/osiam-resource-server}")
    private String resourceServerHome;

    @Value("${org.osiam.auth-server.home:http://localhost:8080/osiam-auth-server}")
    private String authServerHome;

    @Value("${org.osiam.connector.legacy-schemas:false}")
    private boolean useLegacySchemas;

    @Value("${org.osiam.mail.from:selfadmin@localhost}")
    private String fromAddress;

    @Value("${org.osiam.mail.server.host.name:localhost}")
    private String mailServerHost;

    @Value("${org.osiam.mail.server.smtp.port:25}")
    private int mailServerPort;

    @Value("${org.osiam.mail.server.username:}")
    private String mailServerUserName;

    @Value("${org.osiam.mail.server.password:}")
    private String mailServerPassword;

    @Value("${org.osiam.mail.server.transport.protocol:smtp}")
    private String mailServerProtocol;

    @Value("${org.osiam.mail.server.smtp.auth:false}")
    private boolean mailServerAuthenticationEnabled;

    @Value("${org.osiam.mail.server.smtp.starttls.enable:false}")
    private boolean mailServerStartTlsEnabled;

    private String[] allAllowedFields;

    private boolean confirmPasswordRequired = true;

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

    public String getOsiamHome() {
        return osiamHome;
    }

    public String getResourceServerHome() {
        return resourceServerHome;
    }

    public String getAuthServerHome() {
        return authServerHome;
    }

    public boolean useLegacySchemas() {
        return useLegacySchemas;
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

    public String getMailServerHost() {
        return mailServerHost;
    }

    public int getMailServerPort() {
        return mailServerPort;
    }

    public String getMailServerUserName() {
        return mailServerUserName;
    }

    public String getMailServerPassword() {
        return mailServerPassword;
    }

    public String getMailServerProtocol() {
        return mailServerProtocol;
    }

    public boolean isMailServerAuthenticationEnabled() {
        return mailServerAuthenticationEnabled;
    }

    public boolean isMailServerStartTlsEnabled() {
        return mailServerStartTlsEnabled;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations(
                "classpath:/addon-self-administration/resources/css/");
    }

    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource());
        return validator;
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setBasenames("addon-self-administration/i18n/registration",
                "addon-self-administration/i18n/mail");
        return messageSource;
    }
}
