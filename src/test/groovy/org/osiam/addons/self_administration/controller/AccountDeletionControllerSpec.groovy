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

package org.osiam.addons.self_administration.controller

import javax.servlet.http.HttpServletRequest

import org.osiam.addons.self_administration.service.ConnectorBuilder
import org.osiam.addons.self_administration.template.RenderAndSendEmail
import org.osiam.client.OsiamConnector
import org.osiam.client.exception.NoResultException
import org.osiam.client.exception.UnauthorizedException
import org.osiam.client.oauth.AccessToken
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.User
import org.springframework.http.HttpStatus
import org.springframework.mail.MailSendException

import spock.lang.Specification


class AccountDeletionControllerSpec extends Specification {

    ConnectorBuilder connectorBuilder = Mock()
    OsiamConnector osiamConnector = Mock()
    RenderAndSendEmail renderAndSendEmailService = Mock()
    HttpServletRequest servletRequest = Mock()
    AccountManagementService accountManagementService = new AccountManagementService(connectorBuilder: connectorBuilder, renderAndSendEmailService:
    renderAndSendEmailService)

    AccountDeletionController controller = new AccountDeletionController(accountManagementService: accountManagementService)

    def 'A valid request should return HTTP status 200, the user should be deactivated and an email should be sent'() {
        def authHeader = 'Bearer token'
        def userId = 'user ID'
        def userName = 'Joe Random'
        def mailAddress = 'test@osiam.org'
        Email email = new Email.Builder().setValue(mailAddress).setPrimary(true).build();
        User user = new User.Builder(userName).addEmail(email).build()
        AccessToken accessToken = new AccessToken.Builder('token').build()

        when:
        def result = controller.deleteUser(authHeader, userId, servletRequest)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, accessToken) >> user
        1 * osiamConnector.deleteUser(userId, accessToken)
        1 * renderAndSendEmailService.renderAndSendEmail('deletion', _, mailAddress, _, _)
        result.getStatusCode() == HttpStatus.OK
    }

    def 'The request should return HTTP status 401 if the token was not valid'() {
        given:
        def authHeader = 'Bearer invalid'
        def userId = 'user ID'
        def message = 'Do I know you?'
        AccessToken accessToken = new AccessToken.Builder('invalid').build()

        when:
        def result = controller.deleteUser(authHeader, userId, servletRequest)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, accessToken) >> {throw new UnauthorizedException(message)}
        result.getStatusCode() == HttpStatus.UNAUTHORIZED
        result.getBody() == '{\"error\":\"Authorization failed: ' + message + '\"}'
    }

    def 'The request should return HTTP status 404 if the user was not found'() {
        given:
        def authHeader = 'Bearer token'
        def userId = 'user ID'
        def message = 'No such user'
        AccessToken accessToken = new AccessToken.Builder('token').build()

        when:
        def result = controller.deleteUser(authHeader, userId, servletRequest)

        then:
        1 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, accessToken) >> {throw new NoResultException(message)}
        result.getStatusCode() == HttpStatus.NOT_FOUND
        result.getBody() == '{\"error\":\"No such entity: ' + message + '\"}'
    }

    def 'The request should return HTTP status 500 if the primary email was not found'() {
        given:
        def authHeader = 'Bearer token'
        def userId = 'user ID'
        def userName = 'Joe Random'
        def message = "Unable to send email. No email of user " + userName + " found!"
        User user = new User.Builder(userName).build()
        AccessToken accessToken = new AccessToken.Builder('token').build()

        when:
        def result = controller.deleteUser(authHeader, userId, servletRequest)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, accessToken) >> user
        1 * osiamConnector.deleteUser(userId, accessToken)
        result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
        result.getBody() == '{\"error\":\"An exception occurred: ' + message + '\"}'
    }

    def 'The request should return HTTP status 500 if sending the email failed'() {
        given:
        def authHeader = 'Bearer token'
        def userId = 'user ID'
        def userName = 'Joe Random'
        def message = 'Failed to send'
        def mailAddress = 'test@osiam.org'
        Email email = new Email.Builder().setValue(mailAddress).setPrimary(true).build();
        User user = new User.Builder(userName).addEmail(email).build()
        AccessToken accessToken = new AccessToken.Builder('token').build()

        when:
        def result = controller.deleteUser(authHeader, userId, servletRequest)

        then:
        2 * connectorBuilder.createConnector() >> osiamConnector
        1 * osiamConnector.getUser(userId, accessToken) >> user
        1 * osiamConnector.deleteUser(userId, accessToken)
        1 * renderAndSendEmailService.renderAndSendEmail('deletion', _, mailAddress, _, _) >> {throw new MailSendException(message)}
        result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
        result.getBody() == '{\"error\":\"Failed to send email: ' + message + '\"}'
    }
}
