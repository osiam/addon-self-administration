package org.osiam.addons.selfadministration.registration.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.osiam.addons.selfadministration.exception.InvalidAttributeException;
import org.osiam.addons.selfadministration.exception.OsiamException;
import org.osiam.addons.selfadministration.registration.RegistrationExtension;
import org.osiam.addons.selfadministration.registration.RegistrationUser;
import org.osiam.addons.selfadministration.registration.UserConverter;
import org.osiam.addons.selfadministration.service.ConnectorBuilder;
import org.osiam.addons.selfadministration.template.RenderAndSendEmail;
import org.osiam.addons.selfadministration.util.RegistrationHelper;
import org.osiam.client.connector.OsiamConnector;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.Role;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

@Service
public class RegistrationService {

    @Inject
    UserConverter userConverter;

    @Inject
    ConnectorBuilder connectorBuilder;

    @Inject
    private RenderAndSendEmail renderAndSendEmailService;

    @Value("${org.osiam.mail.from}")
    private String fromAddress;

    @Value("${org.osiam.scim.extension.urn}")
    private String internalScimExtensionUrn;

    @Value("${org.osiam.scim.extension.field.activationtoken}")
    private String activationTokenField;

    private String[] allowedFields;
    private String[] allAllowedFields;

    @Value("${org.osiam.html.form.extensions:}")
    private String[] extensions;

    @Value("${org.osiam.html.form.usernameEqualsEmail:true}")
    private boolean usernameEqualsEmail;

    @Value("${org.osiam.html.form.fields:}")
    private void setAllowedFields(String[] allowedFields) {
        List<String> trimedFields = new ArrayList<>();

        for (String field : allowedFields) {
            trimedFields.add(field.trim());
        }

        if (!trimedFields.contains("email")) {
            trimedFields.add("email");
        }

        if (!trimedFields.contains("password")) {
            trimedFields.add("password");
        }

        if (!usernameEqualsEmail && !trimedFields.contains("userName")) {
            trimedFields.add("userName");
        } else if (usernameEqualsEmail && trimedFields.contains("userName")) {
            trimedFields.remove("userName");
        }
        this.allowedFields = trimedFields.toArray(new String[trimedFields.size()]);
    }

    public boolean isUsernameIsAllreadyTaken(String userName) {
        String query = null;
        try {
            query = "filter=" + URLEncoder.encode("userName eq \"" + userName + "\"", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new OsiamException("Could not UTF-8 encode query for user search", "registration.form.error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        SCIMSearchResult<User> queryResult = osiamConnector.searchUsers(query, accessToken);
        return queryResult.getTotalResults() != 0L;
    }

    public User saveRegistrationUser(RegistrationUser registrationUser) {
        User user = userConverter.toScimUser(registrationUser);
        user = createUserForRegistration(user);

        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        return osiamConnector.createUser(user, accessToken);
    }

    /**
     * puts the activation extension and the role USER to the given User
     */
    private User createUserForRegistration(User user) {
        String activationToken = UUID.randomUUID().toString();
        Extension extension = new Extension(internalScimExtensionUrn);
        extension.addOrUpdateField(activationTokenField, activationToken);
        List<Role> roles = new ArrayList<Role>();
        Role role = new Role.Builder().setValue("USER").build();
        roles.add(role);

        User completeUser = new User.Builder(user)
                .setActive(false)
                .setRoles(roles)
                .addExtension(extension)
                .build();

        return completeUser;
    }

    public void sendRegistrationEmail(User user, StringBuffer requestURL) {
        Optional<Email> email = RegistrationHelper.extractSendToEmail(user);
        if (!email.isPresent()) {
            String message = "Could not register user. No email of user " + user.getUserName() + " found!";
            throw new InvalidAttributeException(message, "registration.exception.noEmail");
        }

        requestURL.append("/activation");

        String activationToken = user.getExtension(internalScimExtensionUrn).getFieldAsString(activationTokenField);

        String registrationLink = RegistrationHelper.createLinkForEmail(requestURL.toString(), user.getId(),
                "activationToken", activationToken);

        Map<String, String> mailVariables = new HashMap<>();
        mailVariables.put("registrationLink", registrationLink);

        renderAndSendEmailService.renderAndSendEmail("registration", fromAddress, email.get().getValue(), user,
                mailVariables);
    }

    public User activateUser(String userId, String activationToken) {
        if (Strings.isNullOrEmpty(userId)) {
            throw new InvalidAttributeException("Can't confirm the user. The userid is empty",
                    "activation.exception");
        }
        if (Strings.isNullOrEmpty(activationToken)) {
            throw new InvalidAttributeException("Can't confirm the user " + userId + ". The activation token is empty",
                    "activation.exception");
        }
        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        User user = osiamConnector.getUser(userId, accessToken);

        Extension extension = user.getExtension(internalScimExtensionUrn);
        String activationTokenFieldValue = extension.getFieldAsString(activationTokenField);

        if (!activationTokenFieldValue.equals(activationToken)) {
            throw new InvalidAttributeException("Activation token miss match. Given: " + activationToken + " stored: "
                    + activationTokenFieldValue, "activation.exception");
        }

        UpdateUser updateUser = getPreparedUserForActivation(extension);
        return osiamConnector.updateUser(userId, updateUser, accessToken);
    }

    private UpdateUser getPreparedUserForActivation(Extension extension) {
        UpdateUser updateUser = new UpdateUser.Builder()
                .deleteExtensionField(extension.getUrn(), activationTokenField)
                .updateActive(true).build();

        return updateUser;
    }

    public String[] getAllAllowedFields() {
        if (allAllowedFields == null || allAllowedFields.length == 0) {
            List<String> allFields = new ArrayList<>();

            for (String field : allowedFields) {
                allFields.add(field.trim());
            }
            for (String field : extensions) {
                allFields.add(field.trim());
            }

            this.allAllowedFields = allFields.toArray(new String[allFields.size()]);
        }
        return allAllowedFields;
    }
}
