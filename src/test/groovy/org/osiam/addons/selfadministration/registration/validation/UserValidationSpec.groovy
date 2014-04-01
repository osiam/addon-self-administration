package org.osiam.addons.selfadministration.registration.validation

import org.osiam.addons.selfadministration.registration.RegistrationUser;
import org.osiam.addons.selfadministration.registration.service.RegistrationService;
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

import spock.lang.Specification

class UserValidationSpec  extends Specification {

    RegistrationService registrationService = Mock()
    UserValidator userValidator = new UserValidator(registrationService: registrationService)
    
    def 'validator should have no errors if all fields are valid'() {
        given:
        RegistrationUser user = new RegistrationUser()
        user.email = 'email@email.de'
        user.photo = '/photo.jpg'
        user.userName = 'userName'
        user.password = 'password'
        user.confirmPassword = 'password'
        
        registrationService.passwordLength >> 8
        registrationService.usernameEqualsEmail >> true
        registrationService.isUsernameIsAllreadyTaken(_) >> false 
        
        when:
        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);
    
        then:
        !errors.hasErrors()
    }
    
    def 'validator should have errors if no username was set'() {
        given:
        RegistrationUser user = new RegistrationUser()
        
        when:
        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);
    
        then:
        errors.hasErrors()
        errors.fieldErrors.first().field == 'userName'
    }
    
    def 'validator should have an error if no valid photo URI was set'() {
        given:
        RegistrationUser user = new RegistrationUser()
        user.userName = 'username'
        user.photo = ' '
        
        when:
        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);
    
        then:
        errors.hasErrors()
        errors.fieldErrors.first().field == 'photo'
    }
    
    def 'validator should have an error if no password was set'() {
        given:
        RegistrationUser user = new RegistrationUser()
        user.userName = 'username'
        user.photo = '/photo.jpg'
        user.password = 'pw'
        registrationService.passwordLength >> 8
        
        when:
        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);
    
        then:
        errors.hasErrors()
        errors.fieldErrors.first().field == 'password'
    }
    
    def 'validator should have an error if both password not match'() {
        given:
        RegistrationUser user = new RegistrationUser()
        user.userName = 'username'
        user.photo = '/photo.jpg'
        user.password = 'pw'
        user.confirmPassword = 'pw2'
        registrationService.passwordLength >> 2
        
        when:
        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);
    
        then:
        errors.hasErrors()
        errors.fieldErrors.first().field == 'confirmPassword'
    }
    
    def 'validator should have an error in email if the username already taken and usernameEqualsEmail = true'() {
        given:
        RegistrationUser user = new RegistrationUser()
        user.userName = 'username'
        user.photo = '/photo.jpg'
        user.password = 'pw'
        user.confirmPassword = 'pw'
        registrationService.passwordLength >> 2
        registrationService.usernameEqualsEmail >> true
        registrationService.isUsernameIsAllreadyTaken(_) >> true
        
        when:
        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);
    
        then:
        errors.hasErrors()
        errors.fieldErrors.first().field == 'email'
    }
    
    def 'validator should have an error in username if the username already taken and usernameEqualsEmail = false'() {
        given:
        RegistrationUser user = new RegistrationUser()
        user.userName = 'username'
        user.photo = '/photo.jpg'
        user.password = 'pw'
        user.confirmPassword = 'pw'
        registrationService.passwordLength >> 2
        registrationService.usernameEqualsEmail >> false
        registrationService.isUsernameIsAllreadyTaken(_) >> true
        
        when:
        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);
    
        then:
        errors.hasErrors()
        errors.fieldErrors.first().field == 'userName'
    }
}
