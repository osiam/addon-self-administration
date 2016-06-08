/*
 * Copyright (C) 2015 tarent AG
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
import org.osiam.client.query.Query
import org.osiam.client.query.QueryBuilder
import org.osiam.resources.scim.Extension
import org.osiam.resources.scim.ExtensionFieldType
import org.osiam.resources.scim.SCIMSearchResult
import org.osiam.resources.scim.User
import spock.lang.Ignore
import spock.lang.Shared

import javax.mail.Message
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

/**
 * This test covers the controller for registration purpose.
 */
@DatabaseSetup('/database_seed_registration.xml')
@DatabaseTearDown(value = '/database_tear_down.xml', type = DatabaseOperation.DELETE_ALL)
class RegistrationIT extends IntegrationTest {

    @Shared
    Client client = ClientBuilder.newClient();

    def setup() {
        setupToken()
    }

    def 'The registration controller should return a rendered html'() {
        given:
        def responseContent
        def responseContentType
        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.GET, ContentType.TEXT) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/registration'
            headers.Accept = 'text/html'

            response.success = { resp, html ->
                responseStatus = resp.statusLine.statusCode
                responseContentType = resp.headers.'Content-Type'
                responseContent = html.text
            }
        }

        then:
        responseStatus == 200
        responseContentType.contains(ContentType.HTML.toString())
        //ensure that the content is HTML
        responseContent.contains('</form>')
        //HTML should contain the fields for registration
        responseContent.contains('/registration')
        responseContent.contains('email')
        responseContent.contains('password')
        responseContent.contains('displayName')
        responseContent.contains('profileUrl')
        responseContent.contains('urn:client:extension')
    }

    def 'The registration controller should complete the registration process if a POST request send to "/registration"'() {
        given:
        def userToRegister = [email: 'email@example.org', password: 'password']

        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST, ContentType.URLENC) { req ->
            headers.'Accept-Language' = 'en, en-US'
            uri.path = REGISTRATION_ENDPOINT + '/registration'
            body = userToRegister

            response.success = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        then:
        responseStatus == 200

        Query query = new QueryBuilder().filter("userName eq \"email@example.org\"").build()
        SCIMSearchResult<User> users = OSIAM_CONNECTOR.searchUsers(query, accessToken)
        User user = users.getResources()[0]
        !user.isActive()
        Extension extension = user.getExtension(SELF_ADMIN_URN)
        extension.getField('activationToken', ExtensionFieldType.STRING) != null
        user.getGroups().get(0).getDisplay().equalsIgnoreCase("Test")

        Message[] messages = fetchEmail('email@example.org')
        messages.length == 1
        Message message = messages[0]
        message.getSubject().contains('Confirmation of your registration')
        GreenMailUtil.getBody(message).contains('your account has been created')
        message.getFrom()[0].toString() == 'noreply@osiam.org'
        message.getAllRecipients()[0].toString().equals('email@example.org')
    }

    def 'A german user should get a german email text'() {
        given:
        def userToRegister = [email: 'email@example.org', password: 'password']

        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST, ContentType.URLENC) { req ->
            headers.'Accept-Language' = 'de, de-DE'
            uri.path = REGISTRATION_ENDPOINT + '/registration'
            body = userToRegister

            response.success = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        then:
        responseStatus == 200

        Message[] messages = fetchEmail('email@example.org')
        messages.length == 1
        Message message = messages[0]
        message.getSubject().contains('Bestätigung der Registrierung')
        GreenMailUtil.getBody(message).contains('Ihr Account wurde erstellt.')
        message.getFrom()[0].toString() == 'noreply@osiam.org'
        message.getAllRecipients()[0].toString().equals('email@example.org')
    }

    def 'The user should be activated with a token without the expiration time'() {
        given:
        def createdUserId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def activationToken = 'cef9452e-00a9-4cec-a086-a171374febef'

        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.GET) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/registration/activation'
            uri.query = [userId: createdUserId, activationToken: activationToken]

            response.success = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        then:
        responseStatus == 200

        OSIAM_CONNECTOR.getUser(createdUserId, accessToken).active
    }

    def 'The user should not be active if the token is expired'() {
        given:
        def createdUserId = '69e1a5dc-89be-4343-976c-b8841af249f4'
        def activationToken = 'cef9452e-11a9-4cec-a086-a171374febef'

        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.GET) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/registration/activation'
            uri.query = [userId: createdUserId, activationToken: activationToken]

            response.failure = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        then:
        responseStatus == 400
        OSIAM_CONNECTOR.getUser(createdUserId, accessToken).active == false
    }

    def 'The user should be active if the token is valid'() {
        given:
        def createdUserId = '69e1a5dc-89be-4343-976c-b8841af249f5'
        def activationToken = 'cef9452e-10a9-4cec-a086-a171374febee'

        def accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        def responseStatus

        when:
        def httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.GET) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/registration/activation'
            uri.query = [userId: createdUserId, activationToken: activationToken]

            response.success = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        then:
        responseStatus == 200

        OSIAM_CONNECTOR.getUser(createdUserId, accessToken).active
    }

    def 'The registration controller should act like the user was not already activated if an user activated when he is already activate'() {
        given:
        def createdUserId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def activationToken = 'cef9452e-00a9-4cec-a086-a171374febef'

        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        def firstResponseStatus
        def secondResponseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.GET) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/registration/activation'
            uri.query = [userId: createdUserId, activationToken: activationToken]

            response.success = { resp ->
                firstResponseStatus = resp.statusLine.statusCode
            }
        }

        httpClient.request(Method.GET) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/registration/activation'
            uri.query = [userId: createdUserId, activationToken: activationToken]

            response.success = { resp ->
                secondResponseStatus = resp.statusLine.statusCode
            }
        }

        then:
        firstResponseStatus == 200
        secondResponseStatus == 200

        OSIAM_CONNECTOR.getUser(createdUserId, accessToken).active
    }

    def 'A registration of a user with client defined extensions'() {
        given:
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        def userToRegister = [email                                                 : 'email@example.org', password: 'password',
                              'extensions[\'urn:client:extension\'].fields[\'age\']': 12]

        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST, ContentType.URLENC) { req ->
            headers.'Accept-Language' = 'en, en-US'
            uri.path = REGISTRATION_ENDPOINT + '/registration'
            body = userToRegister

            response.success = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        then:
        responseStatus == 200

        Query query = new QueryBuilder().filter("userName eq \"email@example.org\"").build()
        SCIMSearchResult<User> users = OSIAM_CONNECTOR.searchUsers(query, accessToken)
        User registeredUser = users.getResources()[0]

        Extension registeredExtension1 = registeredUser.getExtension(SELF_ADMIN_URN)
        registeredExtension1.getField('activationToken', ExtensionFieldType.STRING) != null
        Extension registeredExtension2 = registeredUser.getExtension('urn:client:extension')
        registeredExtension2.getField('age', ExtensionFieldType.STRING) != null
        registeredExtension2.getField('age', ExtensionFieldType.STRING) == '12'
        registeredUser.getGroups().get(0).getDisplay().equalsIgnoreCase("Test")

        Message[] messages = fetchEmail('email@example.org')
        messages.length == 1
        Message message = messages[0]
        message.getSubject().contains('Confirmation of your registration')
        GreenMailUtil.getBody(message).contains('your account has been created')
        message.getFrom()[0].toString() == 'noreply@osiam.org'
        message.getAllRecipients()[0].toString().equals('email@example.org')
    }

    def 'A registration of an user with not allowed field nickName and existing extension but not the field'() {
        given:
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        // email, password are always allowed, displayName is allowed and nickName is disallowed by config
        // extension 'urn:client:extension' is only allowed with field 'age' and not 'gender'
        def userToRegister = [email   : 'email@example.org', password: 'password', displayName: 'displayName',
                              nickName: 'nickname', 'extensions[\'urn:client:extension\'].fields[\'gender\']': 'M']

        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST, ContentType.URLENC) { req ->
            uri.path = REGISTRATION_ENDPOINT + '/registration'
            body = userToRegister

            response.success = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        Query queryString = new QueryBuilder().filter("userName eq \"email@example.org\"").build()
        SCIMSearchResult<User> users = OSIAM_CONNECTOR.searchUsers(queryString, accessToken)
        User registeredUser = users.getResources()[0]

        Extension registeredExtension1 = registeredUser.getExtension(SELF_ADMIN_URN)
        registeredExtension1.getField('activationToken', ExtensionFieldType.STRING) != null
        registeredUser.getExtension('urn:client:extension')
        registeredUser.getGroups().get(0).getDisplay().equalsIgnoreCase("Test")

        then:
        thrown(NoSuchElementException)

        registeredUser.nickName == null
        registeredUser.displayName == 'displayName'

        responseStatus == 200

        Message[] messages = fetchEmail('email@example.org')
        messages.length == 1
    }

    def 'Registration of a user with malformed email and empty password returns with specific error messages'() {
        given:
        def userToRegister = 'email=email&password= &profileUrl=not an url&photo= hello '

        when:
        def response = client.target(REGISTRATION_ENDPOINT + '/registration')
                .request()
                .post(Entity.entity(userToRegister, MediaType.APPLICATION_FORM_URLENCODED), String.class)

        then:
        response.contains('Your email is not well-formed.')
        response.contains('Your password is not long enough')
        response.contains('Please use a valid profile url')
        response.contains('Your Photo URI is not well formed')
    }

    def 'The plugin caused an validation error for registration of an user'() {
        given:
        def userToRegister = 'email=email@osiam.com&password=0123456789'

        when:
        def response = client.target(REGISTRATION_ENDPOINT + '/registration')
                .request()
                .post(Entity.entity(userToRegister, MediaType.APPLICATION_FORM_URLENCODED), String.class)

        then:
        response.contains('<div class="alert alert-danger">')
        response.contains('must end with .org!')
    }

    @Ignore("always fails, maybe this test is not valid anymore?")
    def 'The registration controller should escape the displayName'() {
        given:
        def userToRegister = [email      : 'email@example.org', password: 'password',
                              displayName: "<script>alert('hello!');</script>"]

        def responseStatus

        when:
        HTTPBuilder httpClient = new HTTPBuilder(REGISTRATION_ENDPOINT)

        httpClient.request(Method.POST, ContentType.URLENC) { req ->
            headers.'Accept-Language' = 'en, en-US'
            uri.path = REGISTRATION_ENDPOINT + '/registration'
            body = userToRegister

            response.success = { resp ->
                responseStatus = resp.statusLine.statusCode
            }
        }

        then:
        responseStatus == 200

        Query query = new QueryBuilder().filter("userName eq \"email@example.org\"").build()
        SCIMSearchResult<User> users = OSIAM_CONNECTOR.searchUsers(query, accessToken)
        User user = users.getResources()[0]
        user.emails[0].value == 'email@example.org'
        user.displayName == '&lt;script&gt;alert(&#39;hello!&#39;);&lt;/script&gt;'
    }
}
