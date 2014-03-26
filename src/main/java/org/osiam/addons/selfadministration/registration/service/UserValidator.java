package org.osiam.addons.selfadministration.registration.service;

import org.osiam.addons.selfadministration.registration.RegistrationUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.google.common.base.Strings;

@Component
public class UserValidator implements Validator{

    @Value("${org.osiam.html.form.usernameEqualsEmail}")
    private boolean usernameEqualsEmail;

    @Override
    public boolean supports(Class<?> clazz) {
        return RegistrationUser.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegistrationUser registrationUser = (RegistrationUser) target;
        boolean isUserNameEmpty = Strings.isNullOrEmpty(registrationUser.getUserName());
        if (!usernameEqualsEmail && isUserNameEmpty) {
            errors.rejectValue("userName", "registration.exception.usernameEqualsEmail", "Username not set");
        }
    }
}