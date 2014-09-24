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

package org.osiam.addons.self_administration.controller

import javax.servlet.ServletContext
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse

import org.osiam.addons.self_administration.exception.OsiamException
import org.osiam.addons.self_administration.mail.SendEmail
import org.osiam.addons.self_administration.service.ConnectorBuilder
import org.osiam.addons.self_administration.template.EmailTemplateRenderer
import org.osiam.addons.self_administration.template.RenderAndSendEmail
import org.osiam.addons.self_administration.util.UserObjectMapper
import org.osiam.client.OsiamConnector
import org.osiam.client.exception.OsiamRequestException
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.Extension
import org.osiam.resources.scim.User
import org.springframework.http.HttpStatus

import spock.lang.Specification

/**
 * Test for LostPasswordController
 */
class LostPasswordControllerSpec extends Specification {

    UserObjectMapper mapper = new UserObjectMapper()
    
    def contextMock = Mock(ServletContext)

    def urn = 'urn:scim:schemas:osiam:1.0:Registration'

    def oneTimePasswordField = 'oneTimePassword'

    SendEmail sendMailService = Mock()
    EmailTemplateRenderer emailTemplateRendererService = Mock()
    RenderAndSendEmail renderAndSendEmailService = new RenderAndSendEmail(sendMailService: sendMailService,
    emailTemplateRendererService: emailTemplateRendererService)

    def passwordlostLinkPrefix = 'http://localhost:8080'
    def passwordlostMailFrom = 'noreply@example.org'
    def passwordlostMailSubject = 'Subject'

    def clientPasswordChangeUri = 'http://localhost:8080'

    def bootStrapLib = 'http://bootstrap'
    def angularLib = 'http://angular'
    def jqueryLib = 'http://jquery'

    ConnectorBuilder connectorBuilder = Mock()
    OsiamConnector osiamConnector = Mock()

    LostPasswordController lostPasswordController = new LostPasswordController(oneTimePassword: oneTimePasswordField,
    passwordlostLinkPrefix: passwordlostLinkPrefix,
    fromAddress: passwordlostMailFrom, context: contextMock,
    internalScimExtensionUrn : urn,
    clientPasswordChangeUri: clientPasswordChangeUri, mapper: mapper, bootStrapLib: bootStrapLib, angularLib: angularLib,
    jqueryLib: jqueryLib, renderAndSendEmailService: renderAndSendEmailService,
    connectorBuilder : connectorBuilder)

    def 'The controller should start the flow by generating a one time password and send an email to the user'() {
        given:
        def userId = 'someId'
        def authZHeader = 'Bearer ACCESSTOKEN'

        def emailContent = 'nine bytes and one placeholder $PASSWORDLOSTURL and $BOOTSTRAP and $ANGULAR and $JQUERY'
        User user = new User.Builder()
                .addEmails([
                    new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.lost(authZHeader, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * emailTemplateRendererService.renderEmailSubject(_, _, _) >> 'subject'
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> emailContent
        1 * sendMailService.sendHTMLMail(_, _, _, _)

        result.getStatusCode() == HttpStatus.OK
    }

    def 'there should be an failure if the user could not be updated with one time password'(){
        given:
        def userId = 'someId'
        def authZHeader = 'Bearer ACCESSTOKEN'

        when:
        def response = lostPasswordController.lost(authZHeader, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> {throw new OsiamRequestException(400, '')}
        response.getStatusCode() == HttpStatus.BAD_REQUEST
    }

    def 'there should be an failure if no primary email was found'(){
        given:
        def userId = 'someId'
        def authZHeader = 'Bearer ACCESSTOKEN'
        User user = new User.Builder()
                .build()

        when:
        def response = lostPasswordController.lost(authZHeader, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        response.getStatusCode() == HttpStatus.BAD_REQUEST
        response.getBody() != null
    }

    def 'there should be an failure if the email content for confirmation mail was not found'(){
        given:
        def userId = 'someId'
        def authZHeader = 'Bearer ACCESSTOKEN'
        User user = new User.Builder()
                .addEmails([
                    new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def response = lostPasswordController.lost(authZHeader, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> {throw new OsiamException()}
        response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
        response.getBody() != null
    }

    def 'The controller should verify the user and change its password with the client access token'() {
        given:
        def otp = 'someOTP'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        def userId = 'userId'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', otp)
                .setField('tempMail', 'my@mail.com').build()
        User user = new User.Builder().addExtension(extension)
                .addEmails([
                new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByClient(authZHeader, otp, newPassword, userId)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * osiamConnector.getUser(userId, _) >> user

        result.getStatusCode() == HttpStatus.OK
        result.getBody() != null
    }

    def 'The controller should verify the user and change its password'() {
        given:
        def otp = 'someOTP'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', otp)
                .setField('tempMail', 'my@mail.com').build()
        User user = new User.Builder().addExtension(extension)
                .addEmails([
                new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp, newPassword)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * osiamConnector.getCurrentUser(_) >> user

        result.getStatusCode() == HttpStatus.OK
        result.getBody() != null
    }

    def 'If the user will not be found by the given id the response should contain the appropriate status code'() {
        given:
        def otp = 'someOTP'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        def userId = 'not_existing_id'

        when:
        def result = lostPasswordController.changePasswordByClient(authZHeader, otp, newPassword, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, _) >> {throw new OsiamRequestException(409, '')}

        result.getStatusCode() == HttpStatus.CONFLICT
    }

    def 'If the user will not be found by his access token the response should contain the appropriate status code'() {
        given:
        def otp = 'someOTP'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp, newPassword)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> {throw new OsiamRequestException(409, '')}

        result.getStatusCode() == HttpStatus.CONFLICT
    }

    def 'when changing password with the client access token and the one time password mismatched a forbidden status returned'() {
        given:
        def otp = 'invalid one time password'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        def userId = 'not_existing_id'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
            .setField('oneTimePassword', 'someOTP')
            .setField('tempMail', 'my@mail.com').build()
        User user = new User.Builder().addExtension(extension)
                .addEmails([
                    new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        def userById = getUserAsStringWithExtension('Invalid OTP')

        when:
        def result = lostPasswordController.changePasswordByClient(authZHeader, otp, newPassword, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, _) >> user
        result.getStatusCode() == HttpStatus.FORBIDDEN
    }

    def 'when changing password with the user access token and the one time password mismatched a forbidden status returned'() {
        given:
        def otp = 'invalid one time password'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', 'someOTP')
                .setField('tempMail', 'my@mail.com').build()
        User user = new User.Builder().addExtension(extension)
                .addEmails([
                new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        def userById = getUserAsStringWithExtension('Invalid OTP')

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp, newPassword)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> user
        result.getStatusCode() == HttpStatus.FORBIDDEN
    }

    def 'when changing the password as client there should be a failure if the user update with extensions failed'() {
        given:
        def otp = 'someOTP'
        def userId = 'someId'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', 'someOTP')
                .setField('tempMail', 'my@mail.com').build()
        User user = new User.Builder().addExtension(extension)
                .addEmails([
                new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByClient(authZHeader, otp, newPassword, userId)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, _) >> user
        1 * osiamConnector.updateUser(_, _, _) >> {throw new OsiamRequestException(400, '')}
        result.getStatusCode() == HttpStatus.BAD_REQUEST
        result.getBody() != null
    }

    def 'when changing the password as user there should be a failure if the user update with extensions failed'() {
        given:
        def otp = 'someOTP'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', 'someOTP')
                .setField('tempMail', 'my@mail.com').build()
        User user = new User.Builder().addExtension(extension)
                .addEmails([
                new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp, newPassword)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> user
        1 * osiamConnector.updateUser(_, _, _) >> {throw new OsiamRequestException(400, '')}
        result.getStatusCode() == HttpStatus.BAD_REQUEST
        result.getBody() != null
    }

    def 'when changing the password as client there should be a failure if the provided one time password is empty'() {
        when:
        def result = lostPasswordController.changePasswordByClient('authZ', '', 'newPW', 'userId')

        then:
        result.getStatusCode() == HttpStatus.UNAUTHORIZED
    }

    def 'when changing the password as user there should be a failure if the provided one time password is empty'() {
        when:
        def result = lostPasswordController.changePasswordByUser('authZ', '', 'newPW')

        then:
        result.getStatusCode() == HttpStatus.UNAUTHORIZED
    }

    def 'The controller should provide a html form for entering the new password with already known values like otp and user id'(){
        given:
        def servletResponseMock = Mock(HttpServletResponse)
        def servletResponseOutputStream = Mock(ServletOutputStream)
        def otp = 'otp'
        def userId = 'userID'

        def inputStream = new ByteArrayInputStream('some html with placeholder \$CHANGELINK, \$OTP, \$USERID'.bytes)

        when:
        lostPasswordController.lostForm(otp, userId, servletResponseMock)

        then:
        1 * contextMock.getResourceAsStream('/WEB-INF/registration/change_password.html') >> inputStream
        1 * servletResponseMock.getOutputStream() >> servletResponseOutputStream
    }

    def getUserAsStringWithExtension(String otp) {
        def emails = new Email.Builder().setPrimary(true).setValue('email@example.org').build()

        Extension extension = new Extension.Builder(urn)
            .setField('oneTimePassword', otp).build()

        def user = new User.Builder('George')
                .setPassword('password')
                .addEmails([emails])
                .addExtension(extension)
                .setActive(false)
                .build()

        return mapper.writeValueAsString(user)
    }

    def getUserAsStringWithExtensionAndWithoutEmail(String token) {
        Extension extension = new Extension(urn)
        extension.addOrUpdateField('activationToken', token)

        def user = new User.Builder('George')
                .setPassword('password')
                .addExtension(extension)
                .setActive(false)
                .build()

        return mapper.writeValueAsString(user)
    }
}