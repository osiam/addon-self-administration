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

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.osiam.addons.self_administration.Config;
import org.osiam.addons.self_administration.exception.OsiamException;
import org.osiam.addons.self_administration.one_time_token.OneTimeToken;
import org.osiam.addons.self_administration.service.ConnectorBuilder;
import org.osiam.addons.self_administration.template.RenderAndSendEmail;
import org.osiam.addons.self_administration.util.SelfAdministrationHelper;
import org.osiam.addons.self_administration.util.UserObjectMapper;
import org.osiam.client.OsiamConnector;
import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.OsiamRequestException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.helper.SCIMHelper;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * Controller to handle the lost password flow
 */
@Controller
@RequestMapping(value = "/password")
public class LostPasswordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LostPasswordController.class);

    @Autowired
    private UserObjectMapper mapper;

    @Autowired
    private RenderAndSendEmail renderAndSendEmailService;

    @Autowired
    private ConnectorBuilder connectorBuilder;

    @Autowired
    private Config config;

    /**
     * This endpoint generates an one time password and send an confirmation email including the one time password to
     * users primary email
     *
     * @param authorization
     *            authZ header with valid access token
     * @param userId
     *            the user id for whom you want to change the password
     * @return the HTTP status code
     * @throws IOException
     * @throws MessagingException
     */
    @RequestMapping(value = "/lost/{userId}", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> lost(@RequestHeader("Authorization") final String authorization,
            @PathVariable final String userId) throws IOException, MessagingException {

        final OneTimeToken newOneTimePassword = new OneTimeToken();

        final Extension extension = new Extension.Builder(Config.EXTENSION_URN)
                .setField(Config.ONETIME_PASSWORD_FIELD, newOneTimePassword.toString())
                .build();
        final UpdateUser updateUser = new UpdateUser.Builder().updateExtension(extension).build();

        final String token = SelfAdministrationHelper.extractAccessToken(authorization);
        final AccessToken accessToken = new AccessToken.Builder(token).build();
        User updatedUser;
        try {
            updatedUser = connectorBuilder.createConnector().updateUser(userId, updateUser, accessToken);
        } catch (OsiamRequestException e) {
            LOGGER.warn(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(),
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            LOGGER.error(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(updatedUser);
        if (!email.isPresent()) {
            String message = "Could not change password. No email of user " + updatedUser.getUserName() + " found!";
            LOGGER.error(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.BAD_REQUEST);
        }

        String passwordLostLink = SelfAdministrationHelper.createLinkForEmail(config.getPasswordLostLinkPrefix(),
                updatedUser.getId(), "oneTimePassword", newOneTimePassword.getToken());

        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("lostpasswordlink", passwordLostLink);
        mailVariables.put("user", updatedUser);

        Locale locale = SelfAdministrationHelper.getLocale(updatedUser.getLocale());

        try {
            renderAndSendEmailService.renderAndSendEmail("lostpassword", config.getFromAddress(), email.get()
                    .getValue(), locale,
                    mailVariables);
        } catch (OsiamException e) {
            String message = "Problems creating email for lost password: " + e.getMessage();
            LOGGER.error(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Method to get an HTML form with the appropriate input fields for changing the password. Form includes the already
     * known values for userId and otp.
     *
     * @param oneTimePassword
     *            the one time password from confirmation email
     * @param userId
     *            the user id for whom the password change should be
     */
    @RequestMapping(value = "/lostForm", method = RequestMethod.GET)
    public void lostForm(@RequestParam("oneTimePassword") String oneTimePassword,
            @RequestParam("userId") String userId,
            HttpServletResponse response) throws IOException {

        // load the html file as stream and convert to String for replacement
        InputStream inputStream = getClass().getResourceAsStream("/change_password.html");
        String htmlContent = IOUtils.toString(inputStream, "UTF-8");

        // replace all placeholders with appropriate value
        String replacedUri = htmlContent.replace("$CHANGELINK", config.getClientPasswordChangeUri());
        String replacedOtp = replacedUri.replace("$OTP", oneTimePassword);
        String replacedAll = replacedOtp.replace("$USERID", userId);

        // replace all lib links
        replacedAll = replacedAll.replace("$BOOTSTRAP", config.getBootStrapLib());
        replacedAll = replacedAll.replace("$ANGULAR", config.getAngularLib());
        replacedAll = replacedAll.replace("$JQUERY", config.getJqueryLib());

        // convert the String to stream
        InputStream in = IOUtils.toInputStream(replacedAll);

        // set content type and copy html stream content to response output stream
        response.setContentType("text/html");
        IOUtils.copy(in, response.getOutputStream());
    }

    /**
     * Action to change the users password by himself if the preconditions are satisfied.
     *
     * @param authorization
     *            authZ header with valid access token
     * @param oneTimePassword
     *            the previously generated one time password
     * @param newPassword
     *            the new user password
     * @return the response with status code and the updated user if successfully
     * @throws IOException
     */
    @RequestMapping(value = "/change", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> changePasswordByUser(@RequestHeader("Authorization") final String authorization,
            @RequestParam String oneTimePassword,
            @RequestParam String newPassword) throws IOException {
        return changePassword(null, authorization, oneTimePassword, newPassword);
    }

    /**
     * Action to change the users password by the client if the preconditions are satisfied.
     *
     * @param userId
     *            the id of the user which password should changed
     * @param authorization
     *            authZ header with valid access token
     * @param oneTimePassword
     *            the previously generated one time password
     * @param newPassword
     *            the new user password
     * @return the response with status code and the updated user if successfully
     * @throws IOException
     */
    @RequestMapping(value = "/change/{userId}", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> changePasswordByClient(@RequestHeader("Authorization") final String authorization,
            @RequestParam String oneTimePassword,
            @RequestParam String newPassword,
            @PathVariable final String userId) throws IOException {
        return changePassword(userId, authorization, oneTimePassword, newPassword);
    }

    private ResponseEntity<String> changePassword(String userId, String authorization, String oneTimePassword,
            String newPassword) throws IOException {

        if (Strings.isNullOrEmpty(oneTimePassword)) {
            String message = "The submitted one time password is invalid!";
            LOGGER.warn(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
        }

        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = new AccessToken.Builder(SelfAdministrationHelper.extractAccessToken(authorization))
                .build();

        try {

            User user;
            if (Strings.isNullOrEmpty(userId)) {
                user = osiamConnector.getCurrentUser(accessToken);
            } else {
                user = osiamConnector.getUser(userId, accessToken);
            }

            Extension extension = user.getExtension(Config.EXTENSION_URN);
            final OneTimeToken storedOneTimePassword = OneTimeToken.fromString(extension
                    .getFieldAsString(Config.ONETIME_PASSWORD_FIELD));

            if (storedOneTimePassword.isExpired(config.getOneTimePasswordTimeout())) {
                UpdateUser updateUser = new UpdateUser.Builder()
                        .deleteExtensionField(extension.getUrn(), Config.ONETIME_PASSWORD_FIELD)
                        .build();
                osiamConnector.updateUser(userId, updateUser, accessToken);

                String message = "The submitted one time password is invalid!";
                LOGGER.warn(message);
                return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
            }

            if (!storedOneTimePassword.getToken().equals(oneTimePassword)) {
                String message = "The submitted one time password is invalid!";
                LOGGER.warn(message);
                return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
            }

            UpdateUser updateUser = new UpdateUser.Builder()
                    .updatePassword(newPassword)
                    .deleteExtensionField(extension.getUrn(), Config.ONETIME_PASSWORD_FIELD)
                    .build();

            User updatedUser = osiamConnector.updateUser(user.getId(), updateUser, accessToken);

            return new ResponseEntity<>(mapper.writeValueAsString(updatedUser), HttpStatus.OK);
        } catch (OsiamRequestException e) {
            LOGGER.warn(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(),
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            LOGGER.error(e.getMessage());
            return SelfAdministrationHelper.createErrorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoSuchElementException e) {
            String message = "The submitted one time password is invalid!";
            LOGGER.warn(message);
            return SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN);
        }
    }
}
