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

package org.osiam.addons.self_administration.util

import com.google.common.base.Optional
import org.joda.time.Duration
import org.osiam.resources.helper.SCIMHelper
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Unroll

class SelfAdministrationHelperSpec extends Specification {

    def 'getting primary email from user'() {
        given:
        def thePrimaryMail = 'primary@mail.com'

        Email theEmail = new Email.Builder().setPrimary(true).setValue(thePrimaryMail).build()
        User user = new User.Builder('theMan').addEmails([theEmail] as List).build()

        when:
        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(user)

        then:
        email.isPresent()
        email.get().value == thePrimaryMail
    }

    def 'should return not null if no primary email was found'() {
        given:
        def thePrimaryMail = 'primary@mail.com'

        Email theEmail = new Email.Builder().setPrimary(false).setValue(thePrimaryMail).build()
        User user = new User.Builder('theMan').addEmails([theEmail] as List).build()

        when:
        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(user)

        then:
        email.isPresent()
        email.get().value == 'primary@mail.com'
    }

    def 'should not throw exception if users emails are not present'() {
        given:
        User user = new User.Builder('theMan').build()

        when:
        Optional<Email> email = SCIMHelper.getPrimaryOrFirstEmail(user)

        then:
        !email.isPresent()
    }

    def 'should create a link'() {
        given:
        def linkPrefix = 'http://www.example.com/'
        def userId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def parameterName = 'irrelevant'
        def parameter = 'parameter'

        when:
        def link = SelfAdministrationHelper.createLinkForEmail(linkPrefix, userId, parameterName, parameter)

        then:
        link.contains(linkPrefix)
        link.contains(userId)
        link.contains(parameterName)
        link.contains(parameter)
    }

    def 'should create a response error string entity'() {
        given:
        def message = 'error message'
        def jsonErrorMessage = '{"error":"' + message + '"}'

        when:
        ResponseEntity<String> responseEntity = SelfAdministrationHelper.createErrorResponseEntity(message, HttpStatus.FORBIDDEN)

        then:
        responseEntity.getBody() == jsonErrorMessage
        responseEntity.getStatusCode() == HttpStatus.FORBIDDEN
    }

    @Unroll
    def 'makeDuration converts "#value" to Duration "#expectedDuration"'() {
        when:
        Duration duration = SelfAdministrationHelper.makeDuration(value)

        then:
        duration == expectedDuration

        where:
        value             | expectedDuration
        '1d 1h 1m 1s'     | Duration.millis(90061000)
        '1d 1h 1m'        | Duration.millis(90060000)
        '1d 1h 1s'        | Duration.millis(90001000)
        '1d 1m 1s'        | Duration.millis(86461000)
        '1d 1s'           | Duration.millis(86401000)
        '1d 1m'           | Duration.millis(86460000)
        '1d 1h'           | Duration.millis(90000000)
        '1d'              | Duration.standardDays(1)
        '1h 1m 1s'        | Duration.millis(3661000)
        '1h 1m'           | Duration.millis(3660000)
        '1h 1s'           | Duration.millis(3601000)
        '1h'              | Duration.standardHours(1)
        '1m 1s'           | Duration.millis(61000)
        '1m'              | Duration.standardMinutes(1)
        '1s'              | Duration.standardSeconds(1)

        '24h'             | Duration.standardDays(1)
        '72h'             | Duration.standardDays(3)
        '2880m'           | Duration.standardDays(2)
        '40h 133m 774s'   | Duration.millis(152754000)
        '40h133m774s'     | Duration.millis(152754000)
        '2d2h30m10s'      | Duration.millis(181810000)
        '24h '            | Duration.standardDays(1)
        '   72h   '       | Duration.standardDays(3)
        ' 40h 133m 774s ' | Duration.millis(152754000)
        ''                | Duration.ZERO
    }

    @Unroll
    def 'makeDuration fails to convert "#value" to Duration'() {
        when:
        SelfAdministrationHelper.makeDuration(value)

        then:
        thrown(IllegalArgumentException)

        where:
        value << ['1yr', '1wk', '40h  133m    774s']
    }

}
