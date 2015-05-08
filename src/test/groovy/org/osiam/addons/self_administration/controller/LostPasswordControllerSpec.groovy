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

import org.joda.time.Duration
import org.osiam.addons.self_administration.exception.OsiamException
import org.osiam.addons.self_administration.mail.SendEmail
import org.osiam.addons.self_administration.service.ConnectorBuilder
import org.osiam.addons.self_administration.template.EmailTemplateRenderer
import org.osiam.addons.self_administration.template.RenderAndSendEmail
import org.osiam.addons.self_administration.util.OneTimeToken
import org.osiam.addons.self_administration.util.UserObjectMapper
import org.osiam.client.OsiamConnector
import org.osiam.client.exception.OsiamRequestException
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.Extension
import org.osiam.resources.scim.User
import org.springframework.http.HttpStatus
import spock.lang.Specification

import javax.servlet.ServletContext
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse

/**
 * Test for LostPasswordController
 */
class LostPasswordControllerSpec extends Specification {

    UserObjectMapper mapper = new UserObjectMapper()

    ServletContext contextMock = Mock()

    def urn = 'urn:scim:schemas:osiam:1.0:Registration'

    def oneTimePasswordField = 'oneTimePassword'

    SendEmail sendMailService = Mock()
    EmailTemplateRenderer emailTemplateRendererService = Mock()
    RenderAndSendEmail renderAndSendEmailService = new RenderAndSendEmail(sendMailService: sendMailService,
            emailTemplateRendererService: emailTemplateRendererService)

    def passwordLostLinkPrefix = 'http://localhost:8080'
    def passwordLostMailFrom = 'noreply@example.org'

    def clientPasswordChangeUri = 'http://localhost:8080'

    def bootStrapLib = 'http://bootstrap'
    def angularLib = 'http://angular'
    def jqueryLib = 'http://jquery'

    ConnectorBuilder connectorBuilder = Mock()
    OsiamConnector osiamConnector = Mock()

    LostPasswordController lostPasswordController = new LostPasswordController(
            oneTimePasswordField: oneTimePasswordField, passwordlostLinkPrefix: passwordLostLinkPrefix,
            fromAddress: passwordLostMailFrom, context: contextMock, internalScimExtensionUrn: urn,
            clientPasswordChangeUri: clientPasswordChangeUri, mapper: mapper, bootStrapLib: bootStrapLib,
            angularLib: angularLib, jqueryLib: jqueryLib, renderAndSendEmailService: renderAndSendEmailService,
            connectorBuilder: connectorBuilder, oneTimePasswordTimeout: Duration.standardHours(24))

    def 'The controller should start the flow by generating a one time password and send an email to the user'() {
        given:
        def userId = 'someId'
        def authZHeader = 'Bearer ACCESSTOKEN'

        def emailContent = 'nine bytes and one placeholder $PASSWORDLOSTURL and $BOOTSTRAP and $ANGULAR and $JQUERY'
        User user = new User.Builder()
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
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

    def 'there should be a failure if the user could not be updated with one time password'() {
        given:
        def userId = 'someId'
        def authZHeader = 'Bearer ACCESSTOKEN'

        when:
        def response = lostPasswordController.lost(authZHeader, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> { throw new OsiamRequestException(400, '') }
        response.getStatusCode() == HttpStatus.BAD_REQUEST
    }

    def 'there should be a failure if no primary email was found'() {
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

    def 'there should be a failure if the email content for confirmation mail was not found'() {
        given:
        def userId = 'someId'
        def authZHeader = 'Bearer ACCESSTOKEN'
        User user = new User.Builder()
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def response = lostPasswordController.lost(authZHeader, userId)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> { throw new OsiamException() }
        response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
        response.getBody() != null
    }

    def 'The controller should verify the user and change its password with the client access token'() {
        given:
        OneTimeToken otp = new OneTimeToken()
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        def userId = 'userId'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', otp.toString())
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByClient(authZHeader, otp.token, newPassword, userId)

        then:
        connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * osiamConnector.getUser(userId, _) >> user

        result.getStatusCode() == HttpStatus.OK
        result.getBody() != null
    }

    def 'change password should work with old tokens'() {
        given:
        def otp = 'otp'
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        def userId = 'userId'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', otp)
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByClient(authZHeader, otp, newPassword, userId)

        then:
        connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * osiamConnector.getUser(userId, _) >> user

        result.getStatusCode() == HttpStatus.OK
        result.getBody() != null
    }

    def 'The controller should verify the user and change its password'() {
        given:
        OneTimeToken otp = new OneTimeToken()
        def newPassword = 'newPassword'
        def authZHeader = 'Bearer ACCESSTOKEN'
        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', otp.toString())
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp.token, newPassword)

        then:
        connectorBuilder.createConnector() >> osiamConnector
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
        1 * osiamConnector.getUser(userId, _) >> { throw new OsiamRequestException(409, '') }

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
        1 * osiamConnector.getCurrentUser(_) >> { throw new OsiamRequestException(409, '') }

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
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

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
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp, newPassword)

        then:
        connectorBuilder.createConnector() >> osiamConnector
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
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByClient(authZHeader, otp, newPassword, userId)

        then:
        connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, _) >> user
        1 * osiamConnector.updateUser(_, _, _) >> { throw new OsiamRequestException(400, '') }
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
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp, newPassword)

        then:
        connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> user
        1 * osiamConnector.updateUser(_, _, _) >> { throw new OsiamRequestException(400, '') }
        result.getStatusCode() == HttpStatus.BAD_REQUEST
        result.getBody() != null
    }

    def 'when changing the password as client there should be a failure if the provided one time password is empty'() {
        when:
        def result = lostPasswordController.changePasswordByClient('authZ', '', 'newPW', 'userId')

        then:
        result.getStatusCode() == HttpStatus.FORBIDDEN
    }

    def 'when changing the password as user there should be a failure if the provided one time password is empty'() {
        when:
        def result = lostPasswordController.changePasswordByUser('authZ', '', 'newPW')

        then:
        result.getStatusCode() == HttpStatus.FORBIDDEN
    }

    def 'The controller should provide a html form for entering the new password with already known values like otp and user id'() {
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

    def 'there should be a failure if one time password is expired'() {
        given:
        def authZHeader = 'Bearer ACCESSTOKEN'
        def otp = 'otp'
        def newPassword = 'newPassword'
        def expiredOtp = 'otp:1'

        Extension extension = new Extension.Builder('urn:scim:schemas:osiam:1.0:Registration')
                .setField('oneTimePassword', expiredOtp)
                .build()
        User user = new User.Builder()
                .addExtension(extension)
                .addEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List)
                .build()

        when:
        def result = lostPasswordController.changePasswordByUser(authZHeader, otp, newPassword)

        then:
        connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> user
        result.getStatusCode() == HttpStatus.FORBIDDEN
    }

}
