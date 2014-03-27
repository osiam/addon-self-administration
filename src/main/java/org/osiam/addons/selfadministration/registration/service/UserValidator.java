package org.osiam.addons.selfadministration.registration.service;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.osiam.addons.selfadministration.registration.RegistrationUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.google.common.base.Strings;

@Component
public class UserValidator implements Validator{

    @Inject
    private RegistrationService registrationService;
    
    @Value("${org.osiam.html.form.usernameEqualsEmail:true}")
    private boolean usernameEqualsEmail;
    
    @Value("${org.osiam.html.form.password.length:8}")
    private int passwordLength;

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
        if(!Strings.isNullOrEmpty(registrationUser.getPhoto())){
            try {
                new URI(registrationUser.getPhoto());
            } catch (URISyntaxException e) {
                errors.rejectValue("photo", "registration.exception.photo", "Photo is not an URI");
            }
        }
        if(registrationUser.getPassword().length() < passwordLength){
            String[] argument = {String.valueOf(passwordLength)}; 
            errors.rejectValue("password", "registration.exception.password.length", argument, "Your passwords is not long enough.");
        }
        if(!Strings.isNullOrEmpty(registrationUser.getConfirmPassword())
                && !registrationUser.getConfirmPassword().equals(registrationUser.getPassword())){
            errors.rejectValue("confirmPassword", "registration.exception.password.equality", "Your passwords don't match.");
        }
        String userName;
        String field;
        if(usernameEqualsEmail){
            userName = registrationUser.getEmail();
            field = "email";
        }else{
            userName = registrationUser.getUserName();
            field = "userName";
        }
        if(registrationService.isUsernameIsAllreadyTaken(userName)){
            String[] argument = {field}; 
            errors.rejectValue(field, "registration.exception.username.alreadytaken", argument, "Your " + field + " is already taken");
        }
    }
}