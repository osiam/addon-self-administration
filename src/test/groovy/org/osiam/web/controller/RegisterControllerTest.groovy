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
import org.osiam.client.exception.ConflictException
import org.osiam.client.exception.NoResultException
import org.osiam.client.exception.UnauthorizedException
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.Extension
import org.osiam.resources.scim.User
import org.osiam.web.exception.OsiamException
import org.osiam.web.mail.SendEmail
import org.osiam.web.service.ConnectorBuilder
import org.osiam.web.template.EmailTemplateRenderer
import org.osiam.web.template.RenderAndSendEmail
import org.osiam.web.util.UserObjectMapper
import org.springframework.http.HttpStatus

import spock.lang.Specification

class RegisterControllerTest extends Specification {

    UserObjectMapper mapper = new UserObjectMapper()

    def contextMock = Mock(ServletContext)

    def urn = 'urn:scim:schemas:osiam:1.0:Registration'

    def activationTokenField = 'activationToken'
    def clientRegistrationUri = 'http://someStuff.de/'

    def registermailFrom = 'noreply@example.org'
    def registermailSubject = 'Ihre Registrierung'
    def registermailLinkPrefix = 'https://example.org/register?'

    def bootStrapLib = 'http://bootstrap'
    def angularLib = 'http://angular'

    OsiamConnector osiamConnector = Mock()
    ConnectorBuilder connectorBuilder = Mock()
    SendEmail sendMailService = Mock()
    EmailTemplateRenderer emailTemplateRendererService = Mock()
    RenderAndSendEmail renderAndSendEmailService = new RenderAndSendEmail(sendMailService: sendMailService,
    emailTemplateRendererService: emailTemplateRendererService)

    RegisterController registerController = new RegisterController(context: contextMock,
    clientRegistrationUri: clientRegistrationUri, activationTokenField: activationTokenField,
    fromAddress: registermailFrom, registermailLinkPrefix: registermailLinkPrefix,
    internalScimExtensionUrn: urn,
    mapper: mapper, connectorBuilder: connectorBuilder,
    bootStrapLib: bootStrapLib, angularLib: angularLib,
    renderAndSendEmailService: renderAndSendEmailService)

    def 'The registration controller should return a HTML file as stream'() {
        given:
        def httpServletResponseMock = Mock(HttpServletResponse)
        def inputStream = new ByteArrayInputStream('nine bytes and one placeholder $REGISTERLINK and $BOOTSTRAP and $ANGULAR'.bytes)
        def outputStreamMock = Mock(ServletOutputStream)

        when:
        registerController.index(httpServletResponseMock)

        then:
        1 * httpServletResponseMock.setContentType('text/html')
        1 * contextMock.getResourceAsStream('/WEB-INF/registration/registration.html') >> inputStream
        1 * httpServletResponseMock.getOutputStream() >> outputStreamMock
    }

    def 'The registration controller should activate an previously registered user'(){
        given:
        def userId = UUID.randomUUID().toString()
        def activationToken = UUID.randomUUID().toString()
        User user = getUserWithExtension(activationToken)

        when:
        def response = registerController.activate('Bearer ACCESS_TOKEN', userId, activationToken)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> user
        1 * osiamConnector.updateUser(_, _, _) >> _
        response.getStatusCode() == HttpStatus.OK
    }

    def 'The registration controller should return the status code if the user was not found by his id at activation'(){
        given:
        def userId = UUID.randomUUID().toString()
        def activationToken = UUID.randomUUID().toString()

        when:
        def response = registerController.activate('Bearer ACCESS_TOKEN', userId, activationToken)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> { throw new NoResultException() }
        response.getStatusCode() == HttpStatus.NOT_FOUND
    }

    def 'The registration controller should return the status code if the user was not updated at activation'(){
        given:
        def userId = UUID.randomUUID().toString()
        def activationToken = UUID.randomUUID().toString()
        User user = getUserWithExtension(activationToken)

        when:
        def response = registerController.activate('Bearer ACCESS_TOKEN', userId, activationToken)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> user
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.updateUser(_, _, _) >> { throw new ConflictException() }
        response.getStatusCode() == HttpStatus.CONFLICT
    }

    def 'The registration controller should not activate an previously registered user if wrong activation token is presented'(){
        given:
        def userId = UUID.randomUUID().toString()
        def activationToken = UUID.randomUUID().toString()
        User user = getUserWithExtension(activationToken)

        when:
        def response = registerController.activate('Bearer ACCESS_TOKEN', userId, UUID.randomUUID().toString())

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getCurrentUser(_) >> { throw new UnauthorizedException() }
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
    }

    def 'The registration controller should send a html register-mail'() {
        given:
        def registerMailContent = 'irrelevant'
        def registerSubjectContent = 'irrelevant'
        def auth = 'BEARER ABC=='
        User createdUser = getUserWithExtension('')
        def userString = mapper.writeValueAsString(createdUser)

        when:
        def response = registerController.create(auth, userString)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.createUser(_, _) >> createdUser
        1 * emailTemplateRendererService.renderEmailSubject(_, _, _) >> registerSubjectContent
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> registerMailContent
        1 * sendMailService.sendHTMLMail('noreply@example.org', 'email@example.org', registerSubjectContent, registerMailContent)
        response.statusCode == HttpStatus.OK
    }

    def 'there should be an failure if no primary email was found'() {
        given:
        def auth = 'BEARER ABC=='
        User user = getUserWithExtensionAndWithoutEmail('')
        def userString = mapper.writeValueAsString(user)

        when:
        def response = registerController.create(auth, userString)

        then:
        response.getStatusCode() == HttpStatus.BAD_REQUEST
    }

    def 'there should be an failure if the user could not be updated with activation token'() {
        given:
        def auth = 'BEARER ABC=='
        User user = getUserWithExtension('')
        def userString = mapper.writeValueAsString(user)

        when:
        def response = registerController.create(auth, userString)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.createUser(_, _) >> { throw new UnauthorizedException()}
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
    }

    def 'there should be an failure if the email content for confirmation mail was not found'() {
        given:
        def auth = 'BEARER ABC=='
        User user = getUserWithExtension('')
        def userString = mapper.writeValueAsString(user)
        

        when:
        def response = registerController.create(auth, userString)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.createUser(_, _) >> user
        1 * emailTemplateRendererService.renderEmailSubject(_, _, _) >> 'subject'
        1 * emailTemplateRendererService.renderEmailBody(_, _, _) >> {throw new OsiamException()}
        response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def 'there should be an failure if the provided activation token is empty'() {
        when:
        def result = registerController.activate('authZ', 'userId', '')

        then:
        result.getStatusCode() == HttpStatus.UNAUTHORIZED
    }

    def getUserWithExtension(String token) {
        def emails = new Email.Builder().setPrimary(true).setValue('email@example.org').build()

        Extension extension = new Extension(urn)
        extension.addOrUpdateField('activationToken', token)

        new User.Builder('George')
                .setPassword('password')
                .setEmails([emails])
                .addExtension(extension)
                .setActive(false)
                .build()
    }

    def getUserWithExtensionAndWithoutEmail(String token) {
        Extension extension = new Extension(urn)
        extension.addOrUpdateField('activationToken', token)

        new User.Builder('George')
                .setPassword('password')
                .addExtension(extension)
                .setActive(false)
                .build()
    }
}