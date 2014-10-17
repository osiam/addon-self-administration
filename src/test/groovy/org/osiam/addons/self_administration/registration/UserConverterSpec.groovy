package org.osiam.addons.self_administration.registration

import org.osiam.resources.scim.User
import spock.lang.Specification

class UserConverterSpec extends Specification {

    def 'when converting a user the userName should be escaped but not the password'() {
        given:
        UserConverter userConverter = new UserConverter()

        RegistrationUser registrationUser = new RegistrationUser()
        registrationUser.userName = '<script>alert(\'hello!\');</script>'
        registrationUser.password = '<script>alert(\'hello!\');</script>'

        when:
        User user = userConverter.toScimUser(registrationUser)

        then:
        user.userName == '&lt;script&gt;alert(&#39;hello!&#39;);&lt;/script&gt;'
        user.password == '<script>alert(\'hello!\');</script>'
    }
}
