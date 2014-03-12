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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.osiam.client.connector.OsiamConnector;
import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.OsiamRequestException;
import org.osiam.client.user.BasicUser;
import org.osiam.helper.ObjectMapperWithExtensionConfig;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.ExtensionFieldType;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.osiam.web.exception.OsiamException;
import org.osiam.web.service.ConnectorBuilder;
import org.osiam.web.service.RegistrationExtensionUrnProvider;
import org.osiam.web.template.RenderAndSendEmail;
import org.osiam.web.util.RegistrationHelper;
import org.osiam.web.util.SimpleAccessToken;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger LOGGER = Logger.getLogger(ChangeEmailController.class.getName());

    @Inject
    private RegistrationExtensionUrnProvider registrationExtensionUrnProvider;

    @Inject
    private ObjectMapperWithExtensionConfig mapper;

    @Inject
    private RenderAndSendEmail renderAndSendEmailService;

    @Inject
    private ServletContext context;

    @Inject
    private ConnectorBuilder connectorBuilder;

    /* Extension configuration */
    @Value("${osiam.scim.extension.field.tempemail}")
    private String tempEmail;
    @Value("${osiam.scim.extension.field.emailconfirmtoken}")
    private String confirmationTokenField;
    @Value("${osiam.mail.from}")
    private String fromAddress;

    /* Change email configuration */
    @Value("${osiam.mail.emailchange.linkprefix}")
    private String emailChangeLinkPrefix;

    /* URI for the change email call from JavaScript */
    @Value("${osiam.html.emailchange.url}")
    private String clientEmailChangeUri;

    // css and js libs
    @Value("${osiam.html.dependencies.bootstrap}")
    private String bootStrapLib;
    @Value("${osiam.html.dependencies.angular}")
    private String angularLib;
    @Value("${osiam.html.dependencies.jquery}")
    private String jqueryLib;

    /**
     * Generates a HTTP form with the fields for change email purpose.
     */
    @RequestMapping(method = RequestMethod.GET)
    public void index(HttpServletResponse response) throws IOException {
        // load the html file as stream
        InputStream inputStream = context.getResourceAsStream("/WEB-INF/registration/change_email.html");
        String htmlContent = IOUtils.toString(inputStream, "UTF-8");
        // replacing the url
        String replacedAll = htmlContent.replace("$CHANGELINK", clientEmailChangeUri);

        // replace all lib links
        replacedAll = replacedAll.replace("$BOOTSTRAP", bootStrapLib);
        replacedAll = replacedAll.replace("$ANGULAR", angularLib);
        replacedAll = replacedAll.replace("$JQUERY", jqueryLib);

        InputStream in = IOUtils.toInputStream(replacedAll);
        // set the content type
        response.setContentType("text/html");
        IOUtils.copy(in, response.getOutputStream());
    }

    /**
     * Saving the new E-Mail temporary, generating confirmation token and sending an E-Mail to the old registered
     * address.
     * 
     * @param token
     *        Authorization header with HTTP Bearer authorization and a valid access token
     * @param newEmail
     *        The new email address value
     * @return The HTTP status code
     * @throws IOException
     * @throws MessagingException
     */
    @RequestMapping(method = RequestMethod.POST, value = "/change", produces = "application/json")
    public ResponseEntity<String> change(@RequestHeader final String token,
            @RequestParam final String newEmail) throws IOException, MessagingException {

        User updatedUser;
        String confirmationToken = UUID.randomUUID().toString();
        try {
            updatedUser = getUpdatedUserForEmailChange(token, newEmail, confirmationToken);
        } catch (OsiamRequestException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String activateLink = RegistrationHelper.createLinkForEmail(emailChangeLinkPrefix, updatedUser.getId(),
                "confirmToken", confirmationToken);

        // build the Map with the link for replacement
        Map<String, String> mailVariables = new HashMap<>();
        mailVariables.put("activatelink", activateLink);

        try {
            renderAndSendEmailService.renderAndSendEmail("changeemail", fromAddress, newEmail, updatedUser,
                    mailVariables);
        } catch (OsiamException e) {
            return new ResponseEntity<>("{\"error\":\"Problems creating email for confirming new user email: \""
                    + e.getMessage() + "}", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(mapper.writeValueAsString(updatedUser), HttpStatus.OK);
    }

    /**
     * puts the new email an the confirmation token into the extensions of the user of the given token
     * 
     * @param token
     * @param newEmail
     * @param confirmationToken
     * @return User which has the values in his extension
     */
    private User getUpdatedUserForEmailChange(String token, String newEmail, String confirmationToken) {
        OsiamConnector connector = connectorBuilder.createConnector();
        SimpleAccessToken accessToken = new SimpleAccessToken(token);
        BasicUser user = connector.getCurrentUserBasic(accessToken);
        UpdateUser updateUser = buildUpdateUserForEmailChange(newEmail, confirmationToken);
        User updatedUser = connector.updateUser(user.getId(), updateUser, accessToken);

        return updatedUser;
    }

    /**
     * Validating the confirm token and saving the new email value as primary email if the validation was successful.
     * 
     * @param token
     *        Authorization header with HTTP Bearer authorization and a valid access token
     * @param userId
     *        The user id for the user whom email address should be changed
     * @param confirmToken
     *        The previously generated confirmation token from the confirmation email
     * @return The HTTP status code and the updated user if successful
     */
    @RequestMapping(method = RequestMethod.POST, value = "/confirm", produces = "application/json")
    public ResponseEntity<String> confirm(@RequestHeader final String token, @RequestParam final String userId,
            @RequestParam final String confirmToken) throws IOException, MessagingException {

        if (Strings.isNullOrEmpty(confirmToken)) {
            LOGGER.log(Level.WARNING, "Confirmation token miss match!");
            return new ResponseEntity<>("{\"error\":\"No ongoing email change!\"}", HttpStatus.UNAUTHORIZED);
        }

        User updatedUser;
        Optional<Email> oldEmail;

        try {
            SimpleAccessToken accessToken = new SimpleAccessToken(token);
            User user = connectorBuilder.createConnector().getCurrentUser(accessToken);

            Extension extension = user.getExtension(registrationExtensionUrnProvider.getExtensionUrn());
            String existingConfirmToken = extension.getField(confirmationTokenField, ExtensionFieldType.STRING);

            if (!existingConfirmToken.equals(confirmToken)) {
                LOGGER.log(Level.WARNING, "Confirmation token mismatch!");
                return new ResponseEntity<>("{\"error\":\"No ongoing email change!\"}", HttpStatus.FORBIDDEN);
            }

            String newEmail = extension.getField(tempEmail, ExtensionFieldType.STRING);
            oldEmail = RegistrationHelper.extractSendToEmail(user);

            UpdateUser updateUser = getPreparedUserForEmailChange(extension, newEmail, oldEmail.get());

            updatedUser = connectorBuilder.createConnector().updateUser(userId, updateUser, accessToken);
        } catch (OsiamRequestException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.valueOf(e.getHttpStatusCode()));
        } catch (OsiamClientException e) {
            return new ResponseEntity<>("{\"error\":\"" + e.getMessage() + "\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            renderAndSendEmailService.renderAndSendEmail("changeemailinfo", fromAddress, oldEmail.get().getValue(),
                    updatedUser,
                    new HashMap<String, String>());
        } catch (OsiamException e) {
            return new ResponseEntity<>("{\"error\":\"Problems creating email for confirming new user: \""
                    + e.getMessage() + "}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(mapper.writeValueAsString(updatedUser), HttpStatus.OK);
    }

    private UpdateUser buildUpdateUserForEmailChange(String newEmailValue, String confirmationToken) {

        Extension extension = new Extension(registrationExtensionUrnProvider.getExtensionUrn());
        extension.addOrUpdateField(confirmationTokenField, confirmationToken);
        extension.addOrUpdateField(tempEmail, newEmailValue);

        return new UpdateUser.Builder().updateExtension(extension).build();
    }

    private UpdateUser getPreparedUserForEmailChange(Extension extension, String newEmail,
            Email oldEmail) {
        Set<String> deletionSet = new HashSet<String>();
        deletionSet.add(extension.getUrn() + "." + confirmationTokenField);
        deletionSet.add(extension.getUrn() + "." + tempEmail);

        Email email = new Email.Builder(oldEmail).setValue(newEmail).build();

        UpdateUser updateUser = new UpdateUser.Builder()
                .addEmail(email).deleteEmail(oldEmail)
                .deleteExtensionField(extension.getUrn(), confirmationTokenField)
                .deleteExtensionField(extension.getUrn(), tempEmail).build();

        return updateUser;
    }
}