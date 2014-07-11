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

package org.osiam.addons.selfadministration.util

import org.osiam.resources.helper.SCIMHelper
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.User

import spock.lang.Specification

import com.google.common.base.Optional

class RegistrationHelperSpec extends Specification {

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
    
    def 'should replace old primary email'() {
        given:
        def newPrimaryMail = 'newprimary@mail.com'
        Email oldPrimaryEmail = new Email.Builder().setPrimary(true).setValue('primary@mail.com').build()
        User user = new User.Builder('theMan').addEmails([oldPrimaryEmail] as List).build()
        
        when:
        List<Email> emails = RegistrationHelper.replaceOldPrimaryMail(newPrimaryMail, user.emails)
        
        then:
        emails.find { it.value == newPrimaryMail }
    }
    
    def 'should create a link'() {
        given:
        def linkPrefix = 'http://www.example.com/'
        def userId = 'cef9452e-00a9-4cec-a086-d171374febef'
        def parameterName = 'irrelevant'
        def parameter = 'parameter'
        
        when:
        def link = RegistrationHelper.createLinkForEmail(linkPrefix, userId, parameterName, parameter)
        
        then:
        link.contains(linkPrefix)
        link.contains(userId)
        link.contains(parameterName)
        link.contains(parameter)
    }
}
