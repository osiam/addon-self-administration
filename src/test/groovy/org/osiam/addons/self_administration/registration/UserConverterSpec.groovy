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

package org.osiam.addons.self_administration.registration

import org.osiam.addons.self_administration.Config
import org.osiam.resources.scim.User
import spock.lang.Specification

class UserConverterSpec extends Specification {

    def 'converting a registration user to scim user with username equals email'() {
        given:
        Config config = new Config(usernameEqualsEmail: true)
        UserConverter userConverter = new UserConverter(config: config)

        RegistrationUser registrationUser = new RegistrationUser()
        registrationUser.userName = 'userName'
        registrationUser.email = 'email@email.de'

        when:
        User user = userConverter.toScim(registrationUser)

        then:
        user.userName == 'email@email.de'
        user.emails[0].value == 'email@email.de'
    }

    def 'converting a registration user to scim user'() {
        given:
        Config config = new Config(usernameEqualsEmail: false)
        UserConverter userConverter = new UserConverter(config: config)

        RegistrationUser registrationUser = new RegistrationUser()
        registrationUser.userName = 'userName'
        registrationUser.formattedName = 'formattedName'
        registrationUser.familyName = 'familyName'
        registrationUser.givenName = 'givenName'
        registrationUser.middleName = 'middleName'
        registrationUser.honorificPrefix = 'honorificPrefix'
        registrationUser.honorificSuffix = 'honorificSuffix'
        registrationUser.displayName = 'displayName'
        registrationUser.nickName = 'nickName'
        registrationUser.profileUrl = 'profileUrl'
        registrationUser.title = 'title'
        registrationUser.preferredLanguage = 'preferredLanguage'
        registrationUser.locale = 'locale'
        registrationUser.timezone = 'timezone'
        registrationUser.password = 'password'
        registrationUser.confirmPassword = 'confirmPassword'
        registrationUser.email = 'email@email.de'
        registrationUser.phoneNumber = 'phoneNumber'
        registrationUser.im = 'im'
        registrationUser.photo = 'photo'
        registrationUser.formattedAddress = 'formattedAddress'
        registrationUser.streetAddress = 'streetAddress'
        registrationUser.locality = 'locality'
        registrationUser.region = 'region'
        registrationUser.postalCode = 'postalCode'
        registrationUser.country = 'country'

        when:
        User user = userConverter.toScim(registrationUser)

        then:
        user.userName == 'userName'
        user.name.formatted == 'formattedName'
        user.name.familyName == 'familyName'
        user.name.givenName == 'givenName'
        user.name.middleName == 'middleName'
        user.name.honorificPrefix == 'honorificPrefix'
        user.name.honorificSuffix == 'honorificSuffix'
        user.displayName == 'displayName'
        user.nickName == 'nickName'
        user.profileUrl == 'profileUrl'
        user.title == 'title'
        user.preferredLanguage == 'preferredLanguage'
        user.locale == 'locale'
        user.timezone == 'timezone'
        user.password == 'password'
        user.emails[0].value == 'email@email.de'
        user.phoneNumbers[0].value == 'phoneNumber'
        user.ims[0].value == 'im'
        user.photos[0].valueAsURI.toString() == 'photo'
        user.addresses[0].formatted == 'formattedAddress'
        user.addresses[0].streetAddress == 'streetAddress'
        user.addresses[0].locality == 'locality'
        user.addresses[0].region == 'region'
        user.addresses[0].postalCode == 'postalCode'
        user.addresses[0].country == 'country'
    }

    def 'converting a registration user to scim user without the address'() {
        given:
        Config config = new Config(usernameEqualsEmail: true)
        UserConverter userConverter = new UserConverter(config: config)

        RegistrationUser registrationUser = new RegistrationUser()
        registrationUser.email = 'email@email.de'

        when:
        User user = userConverter.toScim(registrationUser)

        then:
        user.emails[0].value == 'email@email.de'
        user.addresses.size() == 0
    }

    def 'converting a registration user to scim user without multi-valued attributes'() {
        given:
        Config config = new Config(usernameEqualsEmail: false)
        UserConverter userConverter = new UserConverter(config: config)

        RegistrationUser registrationUser = new RegistrationUser()
        registrationUser.userName = 'userName'

        when:
        User user = userConverter.toScim(registrationUser)

        then:
        user.emails.size() == 0
        user.phoneNumbers.size() == 0
        user.ims.size() == 0
        user.photos.size() == 0
        user.addresses.size() == 0
    }
}
