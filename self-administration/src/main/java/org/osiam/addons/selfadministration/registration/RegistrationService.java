/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.addons.selfadministration.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.osiam.addons.selfadministration.exception.InvalidAttributeException;
import org.osiam.addons.selfadministration.service.ConnectorBuilder;
import org.osiam.addons.selfadministration.template.RenderAndSendEmail;
import org.osiam.addons.selfadministration.util.RegistrationHelper;
import org.osiam.client.OsiamConnector;
import org.osiam.client.oauth.AccessToken;
import org.osiam.client.query.Query;
import org.osiam.client.query.QueryBuilder;
import org.osiam.resources.helper.SCIMHelper;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.Role;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

@Service
public class RegistrationService {

    @Inject
    private UserConverter userConverter;

    @Inject
    private ConnectorBuilder connectorBuilder;

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

    @Value("${org.osiam.html.form.password.length:8}")
    private int passwordLength;

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

        String fieldUserName = "userName";
        if (!usernameEqualsEmail && !trimedFields.contains(fieldUserName)) {
            trimedFields.add(fieldUserName);
        } else if (usernameEqualsEmail && trimedFields.contains(fieldUserName)) {
            trimedFields.remove(fieldUserName);
        }
        this.allowedFields = trimedFields.toArray(new String[trimedFields.size()]);
    }

    public boolean isUsernameIsAllreadyTaken(String userName) {
        Query query = new QueryBuilder().filter("userName eq \"" + userName + "\"").build();

        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        SCIMSearchResult<User> queryResult = osiamConnector.searchUsers(query, accessToken);
        return queryResult.getTotalResults() != 0L;
    }
    
    public User convertToScimUser(RegistrationUser registrationUser){
        return userConverter.toScimUser(registrationUser);
    }

    public User saveRegistrationUser(final User user) {
        User registrationUser = createUserForRegistration(user);

        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        return osiamConnector.createUser(registrationUser, accessToken);
    }

    /**
     * puts the activation extension and the role USER to the given User
     */
    private User createUserForRegistration(User user) {
        String activationToken = UUID.randomUUID().toString();
        Extension extension = new Extension.Builder(internalScimExtensionUrn)
                .setField(activationTokenField, activationToken)
                .build();
        List<Role> roles = new ArrayList<Role>();
        Role role = new Role.Builder().setValue("USER").build();
        roles.add(role);

        User completeUser = new User.Builder(user)
                .setActive(false)
                .addRoles(roles)
                .addExtension(extension)
                .build();

        return completeUser;
    }

    public void sendRegistrationEmail(User user, HttpServletRequest request) {
        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(user);
        if (!email.isPresent()) {
            String message = "Could not register user. No email of user " + user.getUserName() + " found!";
            throw new InvalidAttributeException(message, "registration.exception.noEmail");
        }

        StringBuffer requestURL = request.getRequestURL().append("/activation");

        String activationToken = user.getExtension(internalScimExtensionUrn).getFieldAsString(activationTokenField);

        String registrationLink = RegistrationHelper.createLinkForEmail(requestURL.toString(), user.getId(),
                "activationToken", activationToken);

        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("registrationLink", registrationLink);
        mailVariables.put("user", user);

        Locale locale = RegistrationHelper.getLocale(user.getLocale());

        renderAndSendEmailService.renderAndSendEmail("registration", fromAddress, email.get().getValue(), locale,
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

        if(user.isActive()) {
            return user;
        }

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
        return allAllowedFields.clone();
    }

    public int getPasswordLength() {
        return passwordLength;
    }

    public boolean getUsernameEqualsEmail() {
        return usernameEqualsEmail;
    }
}
