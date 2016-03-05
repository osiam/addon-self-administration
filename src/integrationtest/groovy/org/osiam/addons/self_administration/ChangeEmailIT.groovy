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

package org.osiam.addons.self_administration

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseTearDown
import com.icegreen.greenmail.util.GreenMailUtil
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.osiam.client.oauth.AccessToken
import org.osiam.client.oauth.Scope
import org.osiam.resources.scim.Extension
import org.osiam.resources.scim.ExtensionFieldType
import org.osiam.resources.scim.User

import javax.mail.Message

import static groovyx.net.http.ContentType.URLENC

/**
 * Integration test for the change email controller.
 */
@DatabaseSetup('/database_seed_change_email.xml')
@DatabaseTearDown(value = '/database_tear_down.xml', type = DatabaseOperation.DELETE_ALL)
class ChangeEmailIT extends IntegrationTest {

    def setup() {
        setupToken()
    }

    def 'Response with a HTML form to change the current user email'() {
        given:
        def statusCode
        def responseContent
        def responseContentType

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.GET, ContentType.TEXT) {
            uri.path = REGISTRATION_ENDPOINT + '/email'
            headers.Accept = 'text/html'

            response.success = { resp, html ->
                statusCode = resp.statusLine.statusCode
                responseContentType = resp.headers.'Content-Type'
                responseContent = html.text
            }
        }

        then:
        statusCode == 200
        responseContentType.contains(ContentType.HTML.toString())
        responseContent.contains('</form>')
        responseContent.count('ng-model') == 2
        responseContent.contains('url: \'http://localhost:8480\'')
    }

    def 'The requested email change creates confirmation token and saves new email temporary'() {
        given:
        AccessToken accessToken = createAccessToken('GeorgeAlexander', '12345')
        def userId = '7d33bcbe-a54c-43d8-867e-f6146164941e'
        def newEmailValue = 'newEmailForGeorgeAlexander@osiam.org'

        def responseStatusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/email/change'
            send URLENC, [newEmailValue: newEmailValue]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
            headers.'Accept-Language' = 'en, en-US'

            response.success = { resp ->
                responseStatusCode = resp.statusLine.statusCode
            }

            response.failure = { resp ->
                responseStatusCode = resp.statusLine.statusCode
            }
        }

        then:
        responseStatusCode == 200
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.getField('emailConfirmToken', ExtensionFieldType.STRING) != null
        extension.getField('tempMail', ExtensionFieldType.STRING) == newEmailValue

        Message[] messages = fetchEmail(newEmailValue)
        messages.length == 1
        messages[0].getSubject().contains('Confirm your new email address')
        def msg = GreenMailUtil.getBody(messages[0])
        msg.contains('to change your e-mail address, please click the link below:')
        msg.contains(userId)
        msg.contains('George Alexander')
        messages[0].getFrom()[0].toString() == 'noreply@osiam.org'
        messages[0].getAllRecipients()[0].toString().equals('newEmailForGeorgeAlexander@osiam.org')
    }

    def 'The confirmation of the new email removes the old one and set the new one as primary'() {
        given:
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        def userId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def confirmToken = 'cef9452e-00a9-4cec-a086-a171374febef'
        def newEmailValue = 'newEmailForGeorge@osiam.org'

        def savedUserId
        def responseStatusCode
        def temp

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/email/confirm'
            send URLENC, [userId: userId, confirmToken: confirmToken]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
            headers.'Accept-Language' = 'en, en-US'

            response.success = { resp, json ->
                responseStatusCode = resp.statusLine.statusCode
                savedUserId = json.id
            }
        }

        then:
        responseStatusCode == 200
        userId == savedUserId
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.isFieldPresent('emailConfirmToken') == false
        extension.isFieldPresent('tempMail') == false
        user.getEmails().size() == 2
        user.getEmails().each {
            if (it.isPrimary())
                temp = it.getValue()
        }
        temp == newEmailValue
    }

    def 'The confirmation with a non expired token of the new email removes the old one and set the new one as primary'() {
        given:
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        def userId = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def confirmToken = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def newEmailValue = 'newEmailForElisabeth@osiam.org'

        def savedUserId
        def responseStatusCode
        def temp

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/email/confirm'
            send URLENC, [userId: userId, confirmToken: confirmToken]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
            headers.'Accept-Language' = 'en, en-US'

            response.success = { resp, json ->
                responseStatusCode = resp.statusLine.statusCode
                savedUserId = json.id
            }
        }

        then:
        responseStatusCode == 200
        userId == savedUserId
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.isFieldPresent('emailConfirmToken') == false
        extension.isFieldPresent('tempMail') == false
        user.getEmails().size() == 1
        user.getEmails().each {
            if (it.isPrimary())
                temp = it.getValue()
        }
        temp == newEmailValue

        Message[] messages = fetchEmail('elisabeth@osiam.org')
        messages.length == 1
        Message message = messages[0];
        message.getSubject().contains('Your email has been successfully changed')
        GreenMailUtil.getBody(message).contains('your e-mail address has been changed successfully.')
        message.getFrom()[0].toString() == 'noreply@osiam.org'
        message.getAllRecipients()[0].toString().equals('elisabeth@osiam.org')
    }

    def 'The confirmation of the new email fails when token is not valid'() {
        given:
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        def userId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def confirmToken = 'invalid_token'

        def responseStatusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/email/confirm'
            send URLENC, [userId: userId, confirmToken: confirmToken]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
            headers.'Accept-Language' = 'en, en-US'

            response.failure = { resp, json ->
                responseStatusCode = resp.statusLine.statusCode
            }
        }

        then:
        responseStatusCode == 403
    }

    def 'The confirmation of the new email fails when token is expired'() {
        given:
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        def userId = '69e1a5dc-89be-4343-976c-b5541af249f5'
        def confirmToken = '69e1a5dc-89be-4343-976c-b5541af249f5'

        def responseStatusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/email/confirm'
            send URLENC, [userId: userId, confirmToken: confirmToken]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
            headers.'Accept-Language' = 'en, en-US'

            response.failure = { resp ->
                responseStatusCode = resp.statusLine.statusCode
            }
        }

        then:
        responseStatusCode == 403
    }
}
