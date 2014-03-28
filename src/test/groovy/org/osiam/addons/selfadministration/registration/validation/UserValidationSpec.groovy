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
}
