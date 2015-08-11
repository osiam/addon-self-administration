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

package org.osiam.addons.self_administration.validators;

import static com.google.common.base.Strings.isNullOrEmpty;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.osiam.addons.self_administration.Config;
import org.osiam.addons.self_administration.registration.RegistrationService;
import org.osiam.addons.self_administration.service.OsiamService;
import org.osiam.resources.exception.SCIMDataValidationException;
import org.springframework.beans.factory.annotation.Autowired;

public class EmailValidator implements ConstraintValidator<Email, String> {

    @Autowired
    private OsiamService osiamService;

    @Autowired
    private Config config;

    @Override
    public void initialize(Email constraintAnnotation) {
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        if (config.isUsernameEqualsEmail()) {
            if (isNullOrEmpty(email)) {
                return false;
            }

            if (osiamService.isUsernameIsAlreadyTaken(email)) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate(
                        "{registration.validation.email.alreadytaken}").addConstraintViolation();
                return false;
            }
        }
        try {
            new org.osiam.resources.scim.Email.Builder().setValue(email);
        } catch (SCIMDataValidationException e) {
            return false;
        }
        return true;
    }
}
