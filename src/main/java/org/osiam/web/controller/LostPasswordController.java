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

package org.osiam.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.OsiamRequestException;
import org.osiam.helper.ObjectMapperWithExtensionConfig;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.ExtensionFieldType;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.osiam.web.exception.OsiamException;
import org.osiam.web.service.ConnectorBuilder;
import org.osiam.web.template.RenderAndSendEmail;
import org.osiam.web.util.RegistrationHelper;
import org.osiam.web.util.SimpleAccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * Controller to handle the lost password flow
 */
@Controller
@RequestMapping(value = "/password")
public class LostPasswordController {

    private static final Logger LOGGER = Logger.getLogger(LostPasswordController.class.getName());

    @Inject
    private ObjectMapperWithExtensionConfig mapper;

    @Inject
    private RenderAndSendEmail renderAndSendEmailService;

    @Inject
    private ServletContext context;

    @Inject
    private ConnectorBuilder connectorBuilder;

    /* Extension configuration */
    @Value("${osiam.scim.extension.field.onetimepassword}")
    private String oneTimePassword;

    /* Password lost email configuration */
    @Value("${osiam.mail.passwordlost.linkprefix}")
    private String passwordlostLinkPrefix;
    @Value("${osiam.mail.from}")
    private String fromAddress;

    /* URI for the change password call from JavaScript */
    @Value("${osiam.html.passwordlost.url}")
    private String clientPasswordChangeUri;

    // css and js libs
    @Value("${osiam.html.dependencies.bootstrap}")
    private String bootStrapLib;
    @Value("${osiam.html.dependencies.angular}")
    private String angularLib;
    @Value("${osiam.html.dependencies.jquery}")
    private String jqueryLib;

    @Value("${osiam.scim.extension.urn}")
    private String internalScimExtensionUrn;
    
    /**
     * This endpoint generates an one time password and send an confirmation email including the one time password to
     * users primary email
     * 
     * @param authorization
     *        authZ header with valid access token
     * @param userId
     *        the user id for whom you want to change the password
     * @return the HTTP status code
     * @throws IOException
     * @throws MessagingException
     */
    @RequestMapping(value = "/lost/{userId}", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> lost(@RequestHeader final String token, @PathVariable final String userId)
            throws IOException, MessagingException {

        // generate one time password
        String oneTimePassword = UUID.randomUUID().toString();
        UpdateUser updateUser = getPreparedUserForLostPassword(oneTimePassword);

        User updatedUser;
        try {
            updatedUser = connectorBuilder.createConnector().updateUser(userId, updateUser,
                    new SimpleAccessToken(token));
        } catch (OsiamRequestException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Optional<Email> email = RegistrationHelper.extractSendToEmail(updatedUser);
        if (!email.isPresent()) {
            LOGGER.log(Level.WARNING, "Could not change password. No email of user " + updatedUser.getUserName()
                    + " found!");
            return new ResponseEntity<>("{\"error\":\"Could not change password. No email of user "
                    + updatedUser.getUserName() + " found!\"}", HttpStatus.BAD_REQUEST);
        }

        String passwordLostLink = RegistrationHelper.createLinkForEmail(passwordlostLinkPrefix, updatedUser.getId(),
                "oneTimePassword", oneTimePassword);

        Map<String, String> mailVariables = new HashMap<>();
        mailVariables.put("lostpasswordlink", passwordLostLink);

        try {
            renderAndSendEmailService.renderAndSendEmail("lostpassword", fromAddress, email.get().getValue(),
                    updatedUser,
                    mailVariables);
        } catch (OsiamException e) {
            return new ResponseEntity<>("{\"error\":\"Problems creating email for lost password: \"" + e.getMessage()
                    + "}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Method to get an HTML form with the appropriate input fields for changing the password. Form includes the already
     * known values for userId and otp.
     * 
     * @param oneTimePassword
     *        the one time password from confirmation email
     * @param userId
     *        the user id for whom the password change should be
     */
    @RequestMapping(value = "/lostForm", method = RequestMethod.GET)
    public void lostForm(@RequestParam String oneTimePassword, @RequestParam String userId,
            HttpServletResponse response) throws IOException {

        // load the html file as stream and convert to String for replacement
        InputStream inputStream = context.getResourceAsStream("/WEB-INF/registration/change_password.html");
        String htmlContent = IOUtils.toString(inputStream, "UTF-8");

        // replace all placeholders with appropriate value
        String replacedUri = htmlContent.replace("$CHANGELINK", clientPasswordChangeUri);
        String replacedOtp = replacedUri.replace("$OTP", oneTimePassword);
        String replacedAll = replacedOtp.replace("$USERID", userId);

        // replace all lib links
        replacedAll = replacedAll.replace("$BOOTSTRAP", bootStrapLib);
        replacedAll = replacedAll.replace("$ANGULAR", angularLib);
        replacedAll = replacedAll.replace("$JQUERY", jqueryLib);

        // convert the String to stream
        InputStream in = IOUtils.toInputStream(replacedAll);

        // set content type and copy html stream content to response output stream
        response.setContentType("text/html");
        IOUtils.copy(in, response.getOutputStream());
    }

    /**
     * Method to change the users password if the preconditions are satisfied.
     * 
     * @param token
     *        authZ header with valid access token
     * @param oneTimePassword
     *        the previously generated one time password
     * @param newPassword
     *        the new user password
     * @return the response with status code and the updated user if successfully
     * @throws IOException
     */
    @RequestMapping(value = "/change", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> change(@RequestHeader final String token,
            @RequestParam String oneTimePassword,
             @RequestParam String newPassword) throws IOException {

        if (Strings.isNullOrEmpty(oneTimePassword)) {
            LOGGER.log(Level.SEVERE, "The submitted one time password is invalid!");
            return new ResponseEntity<>("{\"error\":\"The submitted one time password is invalid!\"}",
                    HttpStatus.UNAUTHORIZED);
        }

        User updatedUser;
        try {

            SimpleAccessToken accessToken = new SimpleAccessToken(token);
            User user = connectorBuilder.createConnector().getCurrentUser(accessToken);

            // validate the oneTimePassword with the saved one from DB
            Extension extension = user.getExtension(internalScimExtensionUrn);
            String savedOneTimePassword = extension.getField(this.oneTimePassword, ExtensionFieldType.STRING);

            if (!savedOneTimePassword.equals(oneTimePassword)) {
                LOGGER.log(Level.SEVERE, "The submitted one time password is invalid!");
                return new ResponseEntity<>("{\"error\":\"The submitted one time password is invalid!\"}",
                        HttpStatus.FORBIDDEN);
            }

            UpdateUser updateUser = getPreparedUserToChangePassword(extension, newPassword);
            updatedUser = connectorBuilder.createConnector().updateUser(user.getId(), updateUser, accessToken);
        } catch (OsiamRequestException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(mapper.writeValueAsString(updatedUser), HttpStatus.OK);
    }

    private UpdateUser getPreparedUserForLostPassword(String oneTimePassword) {
        Extension extension = new Extension(internalScimExtensionUrn);
        extension.addOrUpdateField(this.oneTimePassword, oneTimePassword);
        return new UpdateUser.Builder().updateExtension(extension).build();
    }

    private UpdateUser getPreparedUserToChangePassword(Extension extension, String newPassword)
            throws JsonProcessingException {
        UpdateUser updateUser = new UpdateUser.Builder().updatePassword(newPassword)
                .deleteExtensionField(extension.getUrn(), oneTimePassword).build();
        return updateUser;
    }
}