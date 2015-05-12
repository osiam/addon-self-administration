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

package org.osiam.addons.self_administration.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.osiam.addons.self_administration.Config;
import org.osiam.addons.self_administration.exception.InvalidAttributeException;
import org.osiam.addons.self_administration.one_time_token.OneTimeToken;
import org.osiam.addons.self_administration.service.ConnectorBuilder;
import org.osiam.addons.self_administration.template.RenderAndSendEmail;
import org.osiam.addons.self_administration.util.SelfAdministrationHelper;
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
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

@Service
public class RegistrationService {

    @Inject
    private ConnectorBuilder connectorBuilder;

    @Inject
    private RenderAndSendEmail renderAndSendEmailService;

    @Inject
    private Config config;

    public boolean isUsernameIsAlreadyTaken(String userName) {
        Query query = new QueryBuilder().filter("userName eq \"" + userName + "\"").build();

        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        SCIMSearchResult<User> queryResult = osiamConnector.searchUsers(query, accessToken);
        return queryResult.getTotalResults() != 0L;
    }

    public User saveRegistrationUser(final User user) {
        Extension extension = new Extension.Builder(config.getExtensionUrn())
                .setField(config.getActivationTokenField(), new OneTimeToken().toString())
                .build();

        List<Role> roles = new ArrayList<Role>();
        Role role = new Role.Builder().setValue("USER").build();
        roles.add(role);

        User registrationUser = new User.Builder(user)
                .setActive(false)
                .addRoles(roles)
                .addExtension(extension)
                .build();

        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        return osiamConnector.createUser(registrationUser, accessToken);
    }

    public void sendRegistrationEmail(User user, HttpServletRequest request) {
        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(user);
        if (!email.isPresent()) {
            String message = "Could not register user. No email of user " + user.getUserName() + " found!";
            throw new InvalidAttributeException(message, "registration.exception.noEmail");
        }

        StringBuffer requestURL = request.getRequestURL().append("/activation");

        final OneTimeToken activationToken = OneTimeToken.fromString(user.getExtension(config.getExtensionUrn())
                .getFieldAsString(config.getActivationTokenField()));

        String registrationLink = SelfAdministrationHelper.createLinkForEmail(requestURL.toString(), user.getId(),
                "activationToken", activationToken.getToken());

        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("registrationLink", registrationLink);
        mailVariables.put("user", user);

        Locale locale = SelfAdministrationHelper.getLocale(user.getLocale());

        renderAndSendEmailService.renderAndSendEmail("registration", config.getFromAddress(), email.get().getValue(),
                locale,
                mailVariables);
    }

    public User activateUser(String userId, String activationTokenToCheck) {
        if (Strings.isNullOrEmpty(userId)) {
            throw new InvalidAttributeException("Can't confirm the user. The userId is empty", "activation.exception");
        }
        if (Strings.isNullOrEmpty(activationTokenToCheck)) {
            throw new InvalidAttributeException("Can't confirm the user " + userId + ". The activation token is empty",
                    "activation.exception");
        }

        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        User user = osiamConnector.getUser(userId, accessToken);

        if (user.isActive()) {
            return user;
        }

        Extension extension = user.getExtension(config.getExtensionUrn());

        final OneTimeToken storedActivationToken = OneTimeToken
                .fromString(extension.getFieldAsString(config.getActivationTokenField()));

        if (storedActivationToken.isExpired(config.getActivationTokenTimeout())) {
            UpdateUser updateUser = new UpdateUser.Builder()
                    .deleteExtensionField(extension.getUrn(), config.getActivationTokenField())
                    .build();
            osiamConnector.updateUser(userId, updateUser, accessToken);

            throw new InvalidAttributeException("Activation token is expired", "activation.exception");
        }

        if (!storedActivationToken.getToken().equals(activationTokenToCheck)) {
            throw new InvalidAttributeException(String.format("Activation token mismatch. Given: %s stored: %s",
                    activationTokenToCheck, storedActivationToken.getToken()),
                    "activation.exception");
        }

        UpdateUser updateUser = new UpdateUser.Builder()
                .deleteExtensionField(extension.getUrn(), config.getActivationTokenField())
                .updateActive(true)
                .build();

        return osiamConnector.updateUser(userId, updateUser, accessToken);
    }
}
