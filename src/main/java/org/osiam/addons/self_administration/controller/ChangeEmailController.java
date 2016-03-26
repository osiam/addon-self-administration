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

package org.osiam.addons.self_administration.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.osiam.addons.self_administration.Config;
import org.osiam.addons.self_administration.exception.OsiamException;
import org.osiam.addons.self_administration.one_time_token.OneTimeToken;
import org.osiam.addons.self_administration.template.RenderAndSendEmail;
import org.osiam.addons.self_administration.util.SelfAdministrationHelper;
import org.osiam.addons.self_administration.util.UserObjectMapper;
import org.osiam.client.OsiamConnector;
import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.OsiamRequestException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.client.user.BasicUser;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.ExtensionFieldType;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * Controller for change E-Mail process.
 */
@Controller
@RequestMapping(value = "/email")
public class ChangeEmailController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeEmailController.class);

    @Autowired
    private UserObjectMapper mapper;

    @Autowired
    private RenderAndSendEmail renderAndSendEmailService;

    @Autowired
    private OsiamConnector osiamConnector;

    @Autowired
    private Config config;

    /**
     * Generates a HTTP form with the fields for change email purpose.
     */
    @RequestMapping(method = RequestMethod.GET)
    public void index(HttpServletResponse response) throws IOException {
        // load the html file as stream
        InputStream inputStream = getClass().getResourceAsStream("/change_email.html");
        String htmlContent = IOUtils.toString(inputStream, "UTF-8");
        // replacing the url
        String replacedAll = htmlContent.replace("$CHANGELINK", config.getClientEmailChangeUri());

        // replace all lib links
        replacedAll = replacedAll.replace("$BOOTSTRAP", config.getBootStrapLib());
        replacedAll = replacedAll.replace("$ANGULAR", config.getAngularLib());
        replacedAll = replacedAll.replace("$JQUERY", config.getJqueryLib());

        InputStream in = IOUtils.toInputStream(replacedAll);
        // set the content type
        response.setContentType("text/html");
        IOUtils.copy(in, response.getOutputStream());
    }

    /**
     * Saving the new E-Mail temporary, generating confirmation token and sending an E-Mail to the old registered
     * address.
     *
     * @param authorization
     *            Authorization header with HTTP Bearer authorization and a valid access token
     * @param newEmailValue
     *            The new email address value
     * @return The HTTP status code
     * @throws IOException
     *             If the updated user object can't mapped to json
     */
    @RequestMapping(method = RequestMethod.POST, value = "/change", produces = "application/json")
    public ResponseEntity<String> change(@RequestHeader("Authorization") final String authorization,
            @RequestParam("newEmailValue") final String newEmailValue) throws IOException {

        User updatedUser;
        final OneTimeToken confirmationToken = new OneTimeToken();
        try {
            updatedUser = getUpdatedUserForEmailChange(SelfAdministrationHelper.extractAccessToken(authorization),
                    newEmailValue, confirmationToken);
        } catch (OsiamRequestException e) {
            LOGGER.warn(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(),
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            LOGGER.error(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String activateLink = SelfAdministrationHelper.createLinkForEmail(config.getEmailChangeLinkPrefix(),
                updatedUser.getId(),
                "confirmToken", confirmationToken.getToken());

        // build the Map with the link for replacement
        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("activatelink", activateLink);
        mailVariables.put("user", updatedUser);

        Locale locale = SelfAdministrationHelper.getLocale(updatedUser.getLocale());

        try {
            renderAndSendEmailService.renderAndSendEmail("changeemail", config.getFromAddress(), newEmailValue, locale,
                    mailVariables);
        } catch (OsiamException e) {
            String message = "Problems creating email for confirming new user: " + e.getMessage();
            LOGGER.error(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(mapper.writeValueAsString(updatedUser), HttpStatus.OK);
    }

    /**
     * Puts the new email an the confirmation token into the extensions of the user of the given token.
     *
     * @param token The string representation of the access token
     * @param newEmail
     * @param confirmationToken
     * @return User which has the values in his extension
     */
    private User getUpdatedUserForEmailChange(String token, String newEmail, OneTimeToken confirmationToken) {
        final AccessToken accessToken = new AccessToken.Builder(token).build();
        final User user = osiamConnector.getMe(accessToken);

        final Extension extension = new Extension.Builder(Config.EXTENSION_URN)
                .setField(Config.CONFIRMATION_TOKEN_FIELD, confirmationToken.toString())
                .setField(Config.TEMP_EMAIL_FIELD, newEmail)
                .build();

        final UpdateUser updateUser = new UpdateUser.Builder()
                .updateExtension(extension)
                .build();

        return osiamConnector.updateUser(user.getId(), updateUser, accessToken);
    }

    /**
     * Validating the confirm token and saving the new email value as primary email if the validation was successful.
     *
     * @param authorization
     *            Authorization header with HTTP Bearer authorization and a valid access token
     * @param userId
     *            The user id for the user whom email address should be changed
     * @param confirmationToken
     *            The previously generated confirmation token from the confirmation email
     * @return The HTTP status code and the updated user if successful
     * @throws IOException
     *
     */
    @RequestMapping(method = RequestMethod.POST, value = "/confirm", produces = "application/json")
    public ResponseEntity<String> confirm(@RequestHeader("Authorization") final String authorization,
            @RequestParam("userId") final String userId,
            @RequestParam("confirmToken") final String confirmationToken) throws IOException {

        if (Strings.isNullOrEmpty(confirmationToken)) {
            String message = "The submitted confirmation token is invalid!";
            LOGGER.warn(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
        }

        User updatedUser;
        Optional<Email> oldEmail;

        try {
            AccessToken accessToken = new AccessToken.Builder(
                    SelfAdministrationHelper.extractAccessToken(authorization)).build();
            User user = osiamConnector.getUser(userId, accessToken);

            Extension extension = user.getExtension(Config.EXTENSION_URN);
            final OneTimeToken storedConfirmationToken = OneTimeToken.fromString(extension
                    .getFieldAsString(Config.CONFIRMATION_TOKEN_FIELD));

            if (storedConfirmationToken.isExpired(config.getConfirmationTokenTimeout())) {
                UpdateUser updateUser = new UpdateUser.Builder()
                        .deleteExtensionField(extension.getUrn(), Config.CONFIRMATION_TOKEN_FIELD)
                        .deleteExtensionField(extension.getUrn(), Config.TEMP_EMAIL_FIELD)
                        .build();
                osiamConnector.updateUser(userId, updateUser, accessToken);

                String message = "The submitted confirmation token is invalid!";
                LOGGER.warn(message);
                return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
            }

            if (!storedConfirmationToken.getToken().equals(confirmationToken)) {
                String message = "The submitted confirmation token is invalid!";
                LOGGER.warn(message);
                return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
            }

            String newEmail = extension.getField(Config.TEMP_EMAIL_FIELD, ExtensionFieldType.STRING);
            oldEmail = user.getPrimaryOrFirstEmail();

            UpdateUser updateUser = getPreparedUserForEmailChange(extension, newEmail, oldEmail.get());

            updatedUser = osiamConnector.updateUser(userId, updateUser, accessToken);
        } catch (OsiamRequestException e) {
            LOGGER.warn(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(),
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            LOGGER.error(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoSuchElementException e) {
            String message = "The submitted confirmation token is invalid!";
            LOGGER.warn(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
        }

        Locale locale = SelfAdministrationHelper.getLocale(updatedUser.getLocale());

        // build the Map with the link for replacement
        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("user", updatedUser);

        try {
            renderAndSendEmailService.renderAndSendEmail("changeemailinfo", config.getFromAddress(), oldEmail.get()
                    .getValue(),
                    locale, mailVariables);
        } catch (OsiamException e) {
            String message = "Problems creating email for confirming new user email: " + e.getMessage();
            LOGGER.error(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(mapper.writeValueAsString(updatedUser), HttpStatus.OK);
    }

    private UpdateUser getPreparedUserForEmailChange(Extension extension, String newEmail, Email oldEmail) {
        Email email = new Email.Builder(oldEmail).setValue(newEmail).build();

        return new UpdateUser.Builder()
                .addEmail(email)
                .deleteEmail(oldEmail)
                .deleteExtensionField(extension.getUrn(), Config.CONFIRMATION_TOKEN_FIELD)
                .deleteExtensionField(extension.getUrn(), Config.TEMP_EMAIL_FIELD)
                .build();
    }
}
