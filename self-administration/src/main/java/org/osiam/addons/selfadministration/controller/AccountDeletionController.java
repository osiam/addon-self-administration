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
package org.osiam.addons.selfadministration.controller;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.osiam.addons.selfadministration.exception.InvalidAttributeException;
import org.osiam.addons.selfadministration.exception.OsiamException;
import org.osiam.addons.selfadministration.service.ConnectorBuilder;
import org.osiam.addons.selfadministration.template.RenderAndSendEmail;
import org.osiam.addons.selfadministration.util.RegistrationHelper;
import org.osiam.client.OsiamConnector;
import org.osiam.client.exception.NoResultException;
import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.UnauthorizedException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.helper.SCIMHelper;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.base.Optional;

/**
 * A controller providing an operation for account deletion.
 */
public class AccountDeletionController {

    private static final Logger LOGGER = Logger.getLogger(AccountDeletionController.class.getName());

    @Inject
    private ConnectorBuilder connectorBuilder;

    @Inject
    private RenderAndSendEmail renderAndSendEmailService;

    @Value("${org.osiam.mail.from}")
    private String fromAddress;

    /**
     * Deletes the user with the given ID.
     * 
     * @param authorization
     *        Authorization header with access token
     * @param userId
     *        the user ID
     * @return the resulting HTTP status code
     */
    @RequestMapping(value = "/deletion/{userId}", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> deleteUser(@RequestHeader("Authorization") final String authorization,
            @PathVariable final String userId, final HttpServletRequest request) {

        AccessToken accessToken = new AccessToken.Builder(RegistrationHelper.extractAccessToken(authorization)).build();

        try {
            User user = getUser(userId, accessToken);
            deleteUser(userId, accessToken);
            sendDeletionEmail(user, request);
        } catch (OsiamClientException | OsiamException | MailException e) {
            return handleException(e);
        }

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    /*
     * Deletes the account of the user with the given user ID.
     */
    private void deleteUser(String userId, AccessToken token) {
        OsiamConnector connector = connectorBuilder.createConnector();

        connector.deleteUser(userId, token);
    }

    /*
     * Returns the SCIM user with the given user ID.
     */
    private User getUser(String userId, AccessToken token) {
        OsiamConnector connector = connectorBuilder.createConnector();

        return connector.getUser(userId, token);
    }

    /*
     * Sends an email informing about the account deactivation.
     */
    private void sendDeletionEmail(User user, HttpServletRequest request) {
        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(user);
        if (!email.isPresent()) {
            String message = "Could not delete user. No email of user " + user.getUserName() + " found!";
            throw new InvalidAttributeException(message, "registration.exception.noEmail");
        }

        Map<String, Object> mailVariables = new HashMap<String, Object>();
        Locale locale = RegistrationHelper.getLocale(user.getLocale());

        renderAndSendEmailService.renderAndSendEmail("deletion", fromAddress, email.get().getValue(), locale,
                mailVariables);
    }

    /*
     * Logs the given exception and returns a suitable response status.
     */
    private ResponseEntity<String> handleException(RuntimeException e) {
        StringBuilder messageBuilder = new StringBuilder();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (e instanceof MailException) {
            messageBuilder.append("Failed to send email: ");
        } else if (e instanceof UnauthorizedException) {
            messageBuilder.append("Authorization failed: ");
            status = HttpStatus.UNAUTHORIZED;
        } else if (e instanceof NoResultException) {
            messageBuilder.append("No such entity: ");
        } else {
            messageBuilder.append("An exception occurred: ");
        }
        LOGGER.log(Level.WARNING, messageBuilder.toString());
        messageBuilder.insert(0, "{\"error\":\"");
        messageBuilder.append(e.getMessage());
        messageBuilder.append("\"}");
        return new ResponseEntity<String>(messageBuilder.toString(), status);
    }
}
