/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * 'Software'), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.web.controller

import javax.servlet.ServletContext
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse

import org.osiam.client.connector.OsiamConnector
import org.osiam.client.exception.UnauthorizedException
import org.osiam.client.user.BasicUser
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.Extension
import org.osiam.resources.scim.User
import org.osiam.web.exception.OsiamException
import org.osiam.web.mail.SendEmail
import org.osiam.web.service.ConnectorBuilder
import org.osiam.web.template.EmailTemplateRenderer
import org.osiam.web.template.RenderAndSendEmail
import org.osiam.web.util.SimpleAccessToken
import org.osiam.web.util.UserObjectMapper
import org.springframework.http.HttpStatus

import spock.lang.Specification

class ChangeEmailControllerSpec extends Specification {

    UserObjectMapper mapper = new UserObjectMapper()

    SendEmail sendMailService = Mock()
    EmailTemplateRenderer emailTemplateRendererService = Mock()
    RenderAndSendEmail renderAndSendEmailService = new RenderAndSendEmail(sendMailService: sendMailService, emailTemplateRendererService: emailTemplateRendererService)

    def urn = 'urn:scim:schemas:osiam:1.0:Registration'

    def confirmTokenField = 'emailConfirmToken'
    def tempMailField = 'tempMail'

    def emailChangeLinkPrefix = 'http://localhost:8080/stuff'
    def emailChangeMailFrom = 'bugs@bunny.com'
    def emailChangeMailSubject = 'email change'
    def emailChangeInfoMailSubject = 'email change done'
    def clientEmailChangeUri = 'http://test'

    def bootStrapLib = 'http://bootstrap'
    def angularLib = 'http://angular'
    def jqueryLib = 'http://jquery'

    def context = Mock(ServletContext)
    ConnectorBuilder connectorBuilder = Mock()
    OsiamConnector osiamConnector = Mock()

    ChangeEmailController changeEmailController = new ChangeEmailController(confirmationTokenField: confirmTokenField,
    tempEmail: tempMailField, context: context, emailChangeLinkPrefix: emailChangeLinkPrefix,
    fromAddress: emailChangeMailFrom, internalScimExtensionUrn: urn,
    mapper: mapper, clientEmailChangeUri: clientEmailChangeUri, bootStrapLib: bootStrapLib, angularLib: angularLib,
    jqueryLib: jqueryLib, renderAndSendEmailService: renderAndSendEmailService,
    connectorBuilder : connectorBuilder)

    def 'there should be an failure in change email if email template file was not found'() {
        given:
        def authZHeader = 'Bearer ACCESSTOKEN'
        def newEmailValue = 'bam@boom.com'
        BasicUser basicUser = new BasicUser()
        User user = new User.Builder().build()

        when:
        def result = changeEmailController.change(authZHeader, newEmailValue)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUserBasic(_) >> basicUser
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * emailTemplateRendererService.renderEmailSubject(_, _, _) >> 'subject'
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> {throw new OsiamException()}
    }

    def 'there should be an failure in change email if email body not found'() {
        given:
        def authZHeader = 'Bearer ACCESSTOKEN'
        def newEmailValue = 'bam@boom.com'
        BasicUser basicUser = new BasicUser()
        User user = new User.Builder().build()

        when:
        def result = changeEmailController.change(authZHeader, newEmailValue)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUserBasic(_) >> basicUser
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * emailTemplateRendererService.renderEmailSubject(_, _, _) >> 'subject'
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> {throw new OsiamException()}
    }

    def 'there should be an failure in change email if email subject not found'() {
        given:
        def authZHeader = 'Bearer ACCESSTOKEN'
        def newEmailValue = 'bam@boom.com'
        BasicUser basicUser = new BasicUser()
        User user = new User.Builder().build()

        when:
        def result = changeEmailController.change(authZHeader, newEmailValue)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUserBasic(_) >> basicUser
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * emailTemplateRendererService.renderEmailSubject(_, _, _) >> {throw new OsiamException()}
    }

    def 'change email should generate a confirmation token, save the new email temporarily and send an email'() {
        given:
        def authZHeader = 'Bearer ACCESSTOKEN'
        def newEmailValue = 'bam@boom.com'
        BasicUser basicUser = new BasicUser()
        User user = new User.Builder().build()

        def emailContent = 'nine bytes and one placeholder $EMAILCHANGEURL and $BOOTSTRAP and $ANGULAR and $JQUERY'

        when:
        def result = changeEmailController.change(authZHeader, newEmailValue)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUserBasic(_) >> basicUser
        1 * osiamConnector.updateUser(_, _, _) >> user
        1 * emailTemplateRendererService.renderEmailSubject(_, _, _) >> 'subject'
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> emailContent
        1 * sendMailService.sendHTMLMail(_, _, _, _)
        result.getStatusCode() == HttpStatus.OK
    }

    def 'should catch UnauthorizedException and returning response with error message'(){
        given:
        def authZ = 'invalid access token'
        SimpleAccessToken accessToken = new SimpleAccessToken('token')

        when:
        def result = changeEmailController.change(authZ, 'some@email.de')

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUserBasic(accessToken) >> {throw new UnauthorizedException('unauthorized')}
        result.getBody() == '{\"error\":\"unauthorized\"}'
    }

    def 'confirm email should validate the confirmation token and save the new email value as primary email and send an email'() {
        given:
        def authZHeader = 'abc'
        def userId = 'userId'
        def confirmToken = 'confToken'
        Extension extension = new Extension('urn:scim:schemas:osiam:1.0:Registration')
        extension.addOrUpdateField('emailConfirmToken', confirmToken)
        extension.addOrUpdateField('tempMail', 'my@mail.com')
        User user = new User.Builder().addExtension(extension)
                .setEmails([new Email.Builder().setValue('email@example.org').setPrimary(true).build()] as List).build()

        def upatedUser = getUpdatedUser()

        def subjectContent = 'email change done'
        def emailContent = 'nine bytes and one placeholder'

        when:
        def result = changeEmailController.confirm(authZHeader, userId, confirmToken)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(_, _) >> user

        result.getStatusCode() == HttpStatus.OK

    }

    def 'there should be an failure if confirmation token miss match'() {
        given:
        def authZHeader = 'abc'
        def userId = 'userId'
        def confirmToken = 'confToken'

        Extension extension = new Extension('urn:scim:schemas:osiam:1.0:Registration')
        extension.addOrUpdateField('emailConfirmToken', 'wrong token')
        extension.addOrUpdateField('tempMail', 'my@mail.com')
        User user = new User.Builder().addExtension(extension)
                .build()

        when:
        def response = changeEmailController.confirm(authZHeader, userId, confirmToken)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(_, _) >> user
        response.getStatusCode() == HttpStatus.FORBIDDEN
    }

    def 'there should be a failure if the provided confirmation token is empty'() {
        when:
        def result = changeEmailController.confirm('authZ', 'userId', '')

        then:
        result.getStatusCode() == HttpStatus.UNAUTHORIZED
    }

    def 'the controller should provide a html form for entering the new email address'() {
        given:
        def servletResponseMock = Mock(HttpServletResponse)
        def servletOutputStream = Mock(ServletOutputStream)
        def inputStream = new ByteArrayInputStream('some html stuff with \$CHANGELINK placeholder'.bytes)

        when:
        changeEmailController.index(servletResponseMock)

        then:
        1 * context.getResourceAsStream('/WEB-INF/registration/change_email.html') >> inputStream
        1 * servletResponseMock.getOutputStream() >> servletOutputStream
    }

    def getUserAsString() {
        def emails = new Email.Builder().setPrimary(true).setValue('email@example.org').build()

        def user = new User.Builder('Boy George')
                .setPassword('password')
                .setEmails([emails])
                .setActive(false)
                .build()

        return mapper.writeValueAsString(user)
    }

    def getUserWithTempEmailAsString(confToken) {
        def primary = new Email.Builder().setPrimary(true).setValue('email@example.org').build()
        def email = new Email.Builder().setPrimary(false).setValue('nonPrimary@example.org').build()


        def extension = new Extension(urn)
        extension.addOrUpdateField(confirmTokenField, confToken)
        extension.addOrUpdateField(tempMailField, 'newemail@example.org')

        def user = new User.Builder('Boy George')
                .setPassword('password')
                .setEmails([primary, email] as List)
                .setActive(false)
                .addExtension(extension)
                .build()

        return mapper.writeValueAsString(user)
    }

    def getUpdatedUser() {
        def primary = new Email.Builder().setPrimary(true).setValue('newemail@example.org').build()
        def email = new Email.Builder().setPrimary(false).setValue('nonPrimary@example.org').build()

        def user = new User.Builder('Boy George')
                .setPassword('password')
                .setEmails([primary, email] as List)
                .setActive(false)
                .build()

        return mapper.writeValueAsString(user)
    }
}