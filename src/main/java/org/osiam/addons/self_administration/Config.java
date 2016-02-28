package org.osiam.addons.self_administration;

import com.google.common.collect.ImmutableList;
import org.osiam.addons.self_administration.exception.ConfigurationException;
import org.osiam.addons.self_administration.registration.HtmlField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    public static final String EXTENSION_URN = "urn:org.osiam:scim:extensions:addon-self-administration";

    public static final String ACTIVATION_TOKEN_FIELD = "activationToken";

    public static final String ONETIME_PASSWORD_FIELD = "oneTimePassword";

    public static final String TEMP_EMAIL_FIELD = "tempMail";

    public static final String CONFIRMATION_TOKEN_FIELD = "emailConfirmToken";

    private static final List<String> FIELDS = ImmutableList.of(
            "userName", "email", "password", "confirmPassword", "formattedName", "familyName", "givenName",
            "middleName", "honorificPrefix", "honorificSuffix", "displayName", "profileUrl", "title",
            "preferredLanguage", "locale", "timezone", "phoneNumber", "im", "photo", "formattedAddress",
            "streetAddress", "locality", "region", "postalCode", "country");

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

    private HtmlField[] allAllowedFields;
    private String[] allowedFieldNames;

    private boolean confirmPasswordRequired = true;

    @Autowired
    public void createAllAllowedFields(@Value("${org.osiam.html.form.fields:}") String[] allowedFields,
                                        @Value("${org.osiam.html.form.extensions:}") String[] extensions) {
        List<String> allowedFieldNames = new ArrayList<>();
        List<HtmlField> collectedFields = new ArrayList<>();

        HtmlField htmlField;
        for (String field : allowedFields) {
            htmlField = new HtmlField();
            String trimmedField = field.trim();
            if (trimmedField.contains(":")) {
                final String[] fieldMapping = trimmedField.split(":");
                if (fieldMapping.length > 3) {
                    throw new ConfigurationException("The field configuration must be in the form field:required e.g." +
                            "'userName:true' or just the field e.g. 'userName', but it is: " + trimmedField);
                }
                htmlField.setName(fieldMapping[0]);
                htmlField.setRequired(Boolean.valueOf(fieldMapping[1]));
                if (fieldMapping.length > 2) {
                    htmlField.setType(fieldMapping[2]);
                }
            } else {
                htmlField.setName(trimmedField);
            }
            if (!FIELDS.contains(htmlField.getName())) {
                LOGGER.warn("The configured attribute '{}' is not available and will be ignored.", htmlField.getName());
            }
            collectedFields.add(htmlField);
        }

        if (!containsField(collectedFields, "email")) {
            htmlField = new HtmlField();
            htmlField.setName("email");
            htmlField.setRequired(true);
            collectedFields.add(htmlField);
        }

        if (!containsField(collectedFields, "password")) {
            htmlField = new HtmlField();
            htmlField.setName("password");
            htmlField.setRequired(true);
            collectedFields.add(htmlField);
        }

        final HtmlField confirmPasswordField = getField(collectedFields, "confirmPassword");
        if (confirmPasswordField != null) {
            confirmPasswordField.setRequired(true);
        } else {
            confirmPasswordRequired = false;
        }

        String fieldUserName = "userName";
        if (!usernameEqualsEmail && !containsField(collectedFields, fieldUserName)) {
            htmlField = new HtmlField();
            htmlField.setName(fieldUserName);
            htmlField.setRequired(false);
            collectedFields.add(htmlField);
        } else if (usernameEqualsEmail && containsField(collectedFields, fieldUserName)) {
            removeField(collectedFields, fieldUserName);
        }

        // Example extensions['urn:client:extension'].fields['age']:true:number
        for (String field : extensions) {
            htmlField = new HtmlField();
            String trimmedField = field.trim();
            if (trimmedField.contains(":")) {
                int lastIndexOf = trimmedField.lastIndexOf(":");
                if (lastIndexOf == -1) {
                    throw new ConfigurationException("The extension configuration must be in the form field:required " +
                            "e.g. 'extensions['urn:client:extension'].fields['age']:true' or just the field e.g. " +
                            "'extensions['urn:client:extension'].fields['age']', but it is: " + trimmedField);
                }

                final String type = trimmedField.substring(lastIndexOf + 1, trimmedField.length());
                final boolean required;
                // urn:required
                if (type.equals("true") || type.equals("false")) {
                    required = Boolean.valueOf(type);
                    htmlField.setRequired(required);
                    htmlField.setName(trimmedField.substring(0, lastIndexOf));
                } else {
                    // urn:required:type
                    final String urnRequired = trimmedField.substring(0, lastIndexOf - 1);
                    lastIndexOf = urnRequired.lastIndexOf(":");
                    final String requiredString = trimmedField.substring(lastIndexOf + 1, urnRequired.length());
                    if (requiredString.equals("true") || requiredString.equals("false")) {
                        htmlField.setRequired(Boolean.valueOf(requiredString));
                        htmlField.setType(type);
                        htmlField.setName(trimmedField.substring(0, lastIndexOf));
                    } else {
                        // just urn
                        htmlField.setName(trimmedField);
                    }
                }
            } else {
                htmlField.setName(trimmedField);
            }
            htmlField.setExtension(true);
            collectedFields.add(htmlField);
        }

        allAllowedFields = collectedFields.toArray(new HtmlField[collectedFields.size()]);
        this.allowedFieldNames = allowedFieldNames.toArray(new String[allowedFieldNames.size()]);
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

    public HtmlField[] getAllAllowedFields() {
        return allAllowedFields.clone();
    }

    public String [] getAllowedFields() {
        return allowedFieldNames.clone();
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

    private boolean containsField(List<HtmlField> htmlFields, String fieldName) {
        for (HtmlField htmlField : htmlFields) {
            if (htmlField.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private HtmlField getField(List<HtmlField> htmlFields, String fieldName) {
        for (HtmlField htmlField : htmlFields) {
            if (htmlField.getName().equals(fieldName)) {
                return htmlField;
            }
        }
        return null;
    }

    private void removeField(List<HtmlField> htmlFields, String fieldName) {
        int i = 0;
        for (HtmlField htmlField : htmlFields) {
            if (htmlField.getName().equals(fieldName)) {
                break;
            }
            i++;
        }
        htmlFields.remove(i);
    }
}
