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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.osiam.addons.self_administration.Config;
import org.osiam.addons.self_administration.exception.InvalidAttributeException;
import org.osiam.addons.self_administration.exception.UserNotRegisteredException;
import org.osiam.addons.self_administration.one_time_token.OneTimeToken;
import org.osiam.addons.self_administration.plugin_api.CallbackException;
import org.osiam.addons.self_administration.plugin_api.CallbackPlugin;
import org.osiam.addons.self_administration.service.OsiamService;
import org.osiam.addons.self_administration.template.RenderAndSendEmail;
import org.osiam.addons.self_administration.util.SelfAdministrationHelper;
import org.osiam.resources.scim.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RegistrationService {

    @Autowired
    private RenderAndSendEmail renderAndSendEmailService;

    @Autowired
    private Config config;

    @Autowired
    private CallbackPlugin callbackPlugin;

    @Autowired
    private OsiamService osiamService;

    public User saveRegistrationUser(final User user) {
        Extension extension = new Extension.Builder(Config.EXTENSION_URN)
                .setField(Config.ACTIVATION_TOKEN_FIELD, new OneTimeToken().toString())
                .build();

        List<Role> roles = new ArrayList<>();
        Role role = new Role.Builder().setValue("USER").build();
        roles.add(role);

        User registrationUser = new User.Builder(user)
                .setActive(false)
                .addRoles(roles)
                .addExtension(extension)
                .build();

        return osiamService.createUser(registrationUser);
    }

    public void sendRegistrationEmail(User user, String requestUrl) {
        Optional<Email> email = user.getPrimaryOrFirstEmail();
        if (!email.isPresent()) {
            String message = "Could not register user. No email of user " + user.getUserName() + " found!";
            throw new InvalidAttributeException(message, "registration.exception.noEmail");
        }

        final OneTimeToken activationToken = OneTimeToken.fromString(user.getExtension(Config.EXTENSION_URN)
                .getFieldAsString(Config.ACTIVATION_TOKEN_FIELD));

        String registrationLink = SelfAdministrationHelper.createLinkForEmail(requestUrl + "/activation", user.getId(),
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

        User user = osiamService.getUser(userId);

        if (user.isActive()) {
            return user;
        }

        Extension extension = user.getExtension(Config.EXTENSION_URN);

        final OneTimeToken storedActivationToken = OneTimeToken
                .fromString(extension.getFieldAsString(Config.ACTIVATION_TOKEN_FIELD));

        if (storedActivationToken.isExpired(config.getActivationTokenTimeout())) {
            UpdateUser updateUser = new UpdateUser.Builder()
                    .deleteExtensionField(extension.getUrn(), Config.ACTIVATION_TOKEN_FIELD)
                    .build();
            osiamService.updateUser(userId, updateUser);

            throw new InvalidAttributeException("Activation token is expired", "activation.exception");
        }

        if (!storedActivationToken.getToken().equals(activationTokenToCheck)) {
            throw new InvalidAttributeException(String.format("Activation token mismatch. Given: %s stored: %s",
                    activationTokenToCheck, storedActivationToken.getToken()),
                    "activation.exception");
        }

        UpdateUser updateUser = new UpdateUser.Builder()
                .deleteExtensionField(extension.getUrn(), Config.ACTIVATION_TOKEN_FIELD)
                .updateActive(true)
                .build();

        return osiamService.updateUser(userId, updateUser);
    }

    public User registerUser(User user, String requestUrl) {
        User savedUser = saveRegistrationUser(user);
        try {
            sendRegistrationEmail(savedUser, requestUrl);
        } catch (Exception e) {
            osiamService.deleteUser(savedUser.getId());
            throw new UserNotRegisteredException();
        }
        return savedUser;
    }

    public void postRegistration(User user) throws CallbackException {
        callbackPlugin.performPostRegistrationActions(user);
    }

    public void preRegistration(User user) throws CallbackException {
        callbackPlugin.performPreRegistrationActions(user);
    }
}
