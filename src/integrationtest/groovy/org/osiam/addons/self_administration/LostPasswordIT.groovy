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
 * Integration test for lost password controller
 */
@DatabaseSetup('/database_seed_lost_password.xml')
@DatabaseTearDown(value = '/database_tear_down.xml', type = DatabaseOperation.DELETE_ALL)
class LostPasswordIT extends IntegrationTest {

    def setup() {
        setupToken()
    }

    def 'Initiate the lost password activation flow'() {
        given:
        def userId = '69e1a5dc-89be-4343-976c-b5541af249f5'
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken("marissa", "koala", Scope.ADMIN)
        def statusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/password/lost/' + userId
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
            headers.'Accept-Language' = 'en, en-US'

            response.success = { resp ->
                statusCode = resp.statusLine.statusCode
            }
        }

        then:
        statusCode == 200
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.getField('oneTimePassword', ExtensionFieldType.STRING) != null

        Message[] messages = fetchEmail('harry@osiam.org')
        messages.length == 1
        Message message = messages[0]
        message.getSubject().contains('Change of your password')
        def msg = GreenMailUtil.getBody(message)
        msg.contains('to reset your password, please click on the following link:')
        msg.contains(userId)
        message.getFrom()[0].toString() == 'noreply@osiam.org'
        message.getAllRecipients()[0].toString().equals('harry@osiam.org')
    }

    def 'As a client I can change the password of an user with a valid onetime password'() {
        given:
        def otp = 'cef9452e-00a9-4cec-a086-a171374febef'
        def userId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def newPassword = 'pulverToastMann'
        def statusCode
        def savedUserId

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.success = { resp, json ->
                statusCode = resp.statusLine.statusCode
                savedUserId = json.id
            }
        }

        then:
        statusCode == 200
        savedUserId == userId
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.isFieldPresent('oneTimePassword') == false
    }

    def 'As a client I can change the password of an user with a non expired onetime password'() {
        given:
        def otp = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def userId = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def newPassword = 'pulverToastMann'
        def statusCode
        def savedUserId

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.success = { resp, json ->
                statusCode = resp.statusLine.statusCode
                savedUserId = json.id
            }
        }

        then:
        statusCode == 200
        savedUserId == userId
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.isFieldPresent('oneTimePassword') == false
    }

    def 'As a client I can not change the password of an user with a already used onetime password'() {
        given:
        def otp = 'cef9452e-00a9-4cec-a086-a171374febef'
        def userId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def newPassword = 'new_password'
        def statusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
        }

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.failure = { resp ->
                statusCode = resp.statusLine.statusCode
            }
        }

        then:
        statusCode == 403
    }

    def 'As a client I can not change the password of an user with a expired onetime password'() {
        given:
        def otp = '69e1a5dc-89be-4343-976c-b5541af249f5'
        def userId = '69e1a5dc-89be-4343-976c-b5541af249f5'
        def newPassword = 'new_password'
        def statusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.failure = { resp ->
                statusCode = resp.statusLine.statusCode
            }
        }

        then:
        statusCode == 403
    }

    def 'As a user I can change my password with a valid onetime password'() {
        given:
        AccessToken accessToken = createAccessToken('George', '1234')
        def otp = 'cef9452e-00a9-4cec-a086-a171374febef'
        def userId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def newPassword = 'pulverToastMann'
        def statusCode
        def savedUserId

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change'
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.success = { resp, json ->
                statusCode = resp.statusLine.statusCode
                savedUserId = json.id
            }
        }

        then:
        statusCode == 200
        savedUserId == userId
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.isFieldPresent('oneTimePassword') == false
    }

    def 'As a user I can change my password with a non expired onetime password'() {
        given:
        AccessToken accessToken = createAccessToken('Elisabeth', '1234')
        def otp = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def userId = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def newPassword = 'pulverToastMann'
        def statusCode
        def savedUserId

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change'
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.success = { resp, json ->
                statusCode = resp.statusLine.statusCode
                savedUserId = json.id
            }
        }

        then:
        statusCode == 200
        savedUserId == userId
        User user = OSIAM_CONNECTOR.getUser(userId, accessToken)
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.isFieldPresent('oneTimePassword') == false
    }

    def 'As a user I can not change my password with a already used onetime password'() {
        given:
        AccessToken accessToken = createAccessToken('Elisabeth', '1234')
        def otp = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def userId = '69e1a5dc-89be-4343-976c-b6641af249f7'
        def newPassword = 'pulverToastMann'
        def statusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()
        }

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.failure = { resp ->
                statusCode = resp.statusLine.statusCode
            }
        }

        then:
        statusCode == 403
    }

    def 'As a user I can not change my password with an expired onetime password'() {
        given:
        AccessToken accessToken = createAccessToken('Harry', '1234')
        def otp = '69e1a5dc-89be-4343-976c-b5541af249f5'
        def userId = '69e1a5dc-89be-4343-976c-b5541af249f5'
        def newPassword = 'new_password'
        def statusCode

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST) {
            uri.path = REGISTRATION_ENDPOINT + '/password/change/' + userId
            send URLENC, [oneTimePassword: otp, newPassword: newPassword]
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.failure = { resp ->
                statusCode = resp.statusLine.statusCode
            }
        }

        then:
        statusCode == 403
    }

    def 'The retrieved html form contains the onetime password and the userId'() {
        given:
        def otp = 'otpVal'
        def userId = 'userIdVal'

        def statusCode
        def responseContentType
        def responseContent

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.GET, ContentType.TEXT) {
            uri.path = REGISTRATION_ENDPOINT + '/password/lostForm'
            uri.query = [oneTimePassword: otp, userId: userId]
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
        responseContent.contains('\$scope.otp = \'otpVal\'')
        responseContent.contains('\$scope.id = \'userIdVal\'')
        responseContent.count('ng-model') == 2
        responseContent.contains('url: \'http://localhost:8480\'')
    }
}
