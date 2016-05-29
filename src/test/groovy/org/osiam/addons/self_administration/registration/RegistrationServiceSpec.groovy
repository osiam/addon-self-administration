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

package org.osiam.addons.self_administration.registration

import org.osiam.addons.self_administration.Config
import org.osiam.addons.self_administration.exception.UserNotRegisteredException
import org.osiam.addons.self_administration.service.OsiamService
import org.osiam.addons.self_administration.template.RenderAndSendEmail
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.User
import org.springframework.mail.MailSendException
import spock.lang.Specification
import org.osiam.addons.self_administration.exception.InvalidAttributeException

class RegistrationServiceSpec extends Specification {
    OsiamService osiamService = Mock()
    Config config = new Config()
    RenderAndSendEmail renderAndSendEmail = Mock()
    RegistrationService registrationService = new RegistrationService(
            renderAndSendEmailService: renderAndSendEmail,
            config: config,
            osiamService: osiamService)

    def 'user registration should should create user and send activation email'() {
        given:
        def userName = 'Joe Random'
        def mailAddress = 'test@osiam.org'
        Email email = new Email.Builder()
                .setValue(mailAddress)
                .setPrimary(true)
                .build()
        User user = new User.Builder(userName)
                .addEmail(email)
                .build()
        def url = "http://localhost/"

        when:
        User resultUser = registrationService.registerUser(user, url)

        then:
        resultUser != null
        1 * osiamService.createUser(_) >> { User registrationUser -> registrationUser }
        1 * renderAndSendEmail.renderAndSendEmail(_, _, _, _, _)
    }

    def 'user registration with broken mail server is not successful and the created user is deleted'() {
        given:
        def userName = 'Joe Random'
        def mailAddress = 'test@osiam.org'
        Email email = new Email.Builder()
                .setValue(mailAddress)
                .setPrimary(true)
                .build()
        User user = new User.Builder(userName)
                .addEmail(email)
                .build()
        def url = 'http://localhost/'

        when:
        registrationService.registerUser(user, url)

        then:
        thrown(UserNotRegisteredException)
        1 * osiamService.createUser(_) >> { User registrationUser -> registrationUser }
        1 * renderAndSendEmail.renderAndSendEmail(_, _, _, _, _) >> { throw new MailSendException("") }
        1 * osiamService.deleteUser(_)
    }

    def 'activate user with id null - expect InvalidAttributeException'() {
        given:
        def userId = null
        def token = "someToken"

        when:
        registrationService.activateUser(userId, token)

        then:
        thrown(InvalidAttributeException)
    }

    def 'activate user with token null - expect InvalidAttributeException'() {
        given:
        def userId = "someId"
        def token = null

        when:
        registrationService.activateUser(userId, token)

        then:
        thrown(InvalidAttributeException)
    }

    def 'activate user who is already active - resulting user should equal input user object'() {
        given:
        def userId = "someId"
        def token = "someToken"

        and: "a fake user object"
        def userName = 'Joe Random'
        def mailAddress = 'test@osiam.org'
        Email email = new Email.Builder()
                .setValue(mailAddress)
                .setPrimary(true)
                .build()
        User user = new User.Builder(userName)
                .setActive(true)
                .addEmail(email)
                .build()

        when:
        def result = registrationService.activateUser(userId, token)

        then:
        1 * osiamService.getUser("someId") >> user
        result == user
    }

}
