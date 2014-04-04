package org.osiam.addons.selfadministration.registration;

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
            errors.rejectValue("userName", "registration.exception.usernameEqualsEmail", "Username not set");
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
                    "Your passwords is not long enough.");
        }
        if (!Strings.isNullOrEmpty(registrationUser.getConfirmPassword())
                && !registrationUser.getConfirmPassword().equals(registrationUser.getPassword())) {
            errors.rejectValue("confirmPassword", "registration.exception.password.equality",
                    "Your passwords don't match.");
        }
        String userName;
        String field;
        if (registrationService.getUsernameEqualsEmail()) {
            userName = registrationUser.getEmail();
            field = "email";
        } else {
            userName = registrationUser.getUserName();
            field = "userName";
        }
        if (registrationService.isUsernameIsAllreadyTaken(userName)) {
            String[] argument = { field };
            errors.rejectValue(field, "registration.exception.username.alreadytaken", argument, "Your " + field
                    + " is already taken");
        }
    }
}