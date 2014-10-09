/*
 * Copyright (C) 2014 tarent AG
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
package org.osiam.addons.self_administration.controller;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.osiam.addons.self_administration.exception.InvalidAttributeException;
import org.osiam.addons.self_administration.service.ConnectorBuilder;
import org.osiam.addons.self_administration.template.RenderAndSendEmail;
import org.osiam.addons.self_administration.util.RegistrationHelper;
import org.osiam.client.OsiamConnector;
import org.osiam.client.exception.NoResultException;
import org.osiam.client.exception.UnauthorizedException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.helper.SCIMHelper;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;

/**
 * Service class for controllers dealing with account management.
 */
@Service
public class AccountManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountManagementService.class);

    @Inject
    protected ConnectorBuilder connectorBuilder;

    @Inject
    protected RenderAndSendEmail renderAndSendEmailService;

    @Value("${org.osiam.mail.from}")
    protected String fromAddress;

    /**
     * Deactivates the account of the user with the given user ID.
     * 
     * @param userId
     *            the ID of the user
     * @param token
     *            the access token
     */
    public void deactivateUser(String userId, AccessToken token) {
        User user = getUser(userId, token);
        UpdateUser updateUser = getUpdateUserForDeactivation();
        getConnector().updateUser(userId, updateUser, token);
        sendEmail(user, "deactivation");
    }

    /**
     * Deletes the account of the user with the given user ID.
     * 
     * @param userId
     *            the ID of the user
     * @param token
     *            the access token
     */
    public void deleteUser(String userId, AccessToken token) {
        User user = getUser(userId, token);
        getConnector().deleteUser(userId, token);
        sendEmail(user, "deletion");
    }

    /**
     * Logs the given exception and returns a suitable response status.
     * 
     * @param e
     *            the exception to handle
     * @param {@link ResponseEntity} with the resulting error information and status code
     */
    public ResponseEntity<String> handleException(RuntimeException e) {
        StringBuilder messageBuilder = new StringBuilder();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (e instanceof MailException) {
            messageBuilder.append("Failed to send email: ");
        } else if (e instanceof UnauthorizedException) {
            messageBuilder.append("Authorization failed: ");
            status = HttpStatus.UNAUTHORIZED;
        } else if (e instanceof NoResultException) {
            messageBuilder.append("No such entity: ");
            status = HttpStatus.NOT_FOUND;
        } else {
            messageBuilder.append("An exception occurred: ");
        }
        LOGGER.error(messageBuilder.toString());
        messageBuilder.insert(0, "{\"error\":\"");
        messageBuilder.append(e.getMessage());
        messageBuilder.append("\"}");
        return new ResponseEntity<String>(messageBuilder.toString(), status);
    }

    /**
     * Returns the SCIM user with the given user ID.
     * 
     * @param userId
     *            the user ID
     * @param token
     *            the access token
     */
    private User getUser(String userId, AccessToken token) {
        OsiamConnector connector = connectorBuilder.createConnector();

        return connector.getUser(userId, token);
    }

    /**
     * Sends an email informing about the account change.
     * 
     * @param user
     *            the user
     * @param template
     *            the email template name
     */
    private void sendEmail(User user, String template) {
        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(user);
        if (!email.isPresent()) {
            String message = "Unable to send email. No email of user " + user.getUserName() + " found!";
            throw new InvalidAttributeException(message, "registration.exception.noEmail");
        }

        Map<String, Object> mailVariables = new HashMap<String, Object>();
        Locale locale = RegistrationHelper.getLocale(user.getLocale());

        renderAndSendEmailService.renderAndSendEmail(template, fromAddress, email.get().getValue(), locale,
                mailVariables);
    }

    /**
     * Returns a new OSIAM connector.
     * 
     * @return {link {@link OsiamConnector}
     */
    private OsiamConnector getConnector() {
        return connectorBuilder.createConnector();
    }

    /*
     * Builds an UpdateUser for the deactivation.
     */
    private UpdateUser getUpdateUserForDeactivation() {
        return new UpdateUser.Builder().updateActive(false).build();
    }
}
