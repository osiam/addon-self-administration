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

package org.osiam.addons.self_administration.registration;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.google.common.base.Strings;

@Component
public class UserValidator implements Validator {

    @Inject
    private RegistrationService registrationService;

    @Override
    public boolean supports(Class<?> clazz) {
        return RegistrationUser.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegistrationUser registrationUser = (RegistrationUser) target;
        boolean isUserNameEmpty = Strings.isNullOrEmpty(registrationUser.getUserName());
        if (!registrationService.getUsernameEqualsEmail() && isUserNameEmpty) {
            errors.rejectValue("userName", "registration.exception.usernameEqualsEmail", "Username not set.");
        }
        if (!Strings.isNullOrEmpty(registrationUser.getPhoto())) {
            try {
                new URI(registrationUser.getPhoto());
            } catch (URISyntaxException e) {
                errors.rejectValue("photo", "registration.exception.photo", "Photo is not an URI");
            }
        }
        if (registrationUser.getPassword().length() < registrationService.getPasswordLength()) {
            String[] argument = { String.valueOf(registrationService.getPasswordLength()) };
            errors.rejectValue("password", "registration.exception.password.length", argument,
                    "Your password is not long enough. Your password has to be at least "
                            + registrationService.getPasswordLength() + " characters.");
        }
        if (!Strings.isNullOrEmpty(registrationUser.getConfirmPassword())
                && !registrationUser.getConfirmPassword().equals(registrationUser.getPassword())) {
            errors.rejectValue("confirmPassword", "registration.exception.password.equality",
                    "Your passwords don't match.");
        }
        if (registrationService.getUsernameEqualsEmail()
                && registrationService.isUsernameIsAllreadyTaken(registrationUser.getEmail())) {
            errors.rejectValue("email", "registration.exception.email.alreadytaken",
                    "Your email address is already taken.");
        } else if (!registrationService.getUsernameEqualsEmail()
                && registrationService.isUsernameIsAllreadyTaken(registrationUser.getUserName())) {
            errors.rejectValue("userName", "registration.exception.username.alreadytaken",
                    "Your username is already taken.");
        }
    }
}