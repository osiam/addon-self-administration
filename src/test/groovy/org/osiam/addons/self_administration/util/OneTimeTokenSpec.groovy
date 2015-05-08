package org.osiam.addons.self_administration.util

import org.joda.time.Duration
import org.osiam.addons.self_administration.one_time_token.OneTimeToken
import spock.lang.Specification
import spock.lang.Unroll

class OneTimeTokenSpec extends Specification {

    @Unroll
    def 'Activation token can be parsed from #value'() {
        when:
        OneTimeToken activationToken = OneTimeToken.fromString(value)

        then:
        activationToken.token == token
        activationToken.issuedTime == issuedTime

        where:
        value           | token   | issuedTime
        'token:8732469' | 'token' | 8732469L
        'token'         | 'token' | Long.MAX_VALUE
    }

    @Unroll
    def 'Activation token cannot be parsed from #value'() {
        when:
        OneTimeToken.fromString(value)

        then:
        thrown(expectedException)

        where:
        value           | expectedException
        'token:invalid' | NumberFormatException
        'token:'        | NumberFormatException
    }

    @Unroll
    def 'isExpired returns #expectedExpired if token has been issued on #issuedTime'() {
        given:
        long tokenTimeout = Duration.standardHours(24).millis
        OneTimeToken activationToken = new OneTimeToken("token", issuedTime)

        expect:
        activationToken.isExpired(tokenTimeout) == expectedExpired

        where:
        issuedTime                 | expectedExpired
        System.currentTimeMillis() | false
        Long.MAX_VALUE             | false
        1000L                      | true
    }
}
