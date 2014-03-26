package org.osiam.addons.selfadministration.registration.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.osiam.addons.selfadministration.registration.RegistrationUser;
import org.osiam.addons.selfadministration.registration.service.RegistrationService;
import org.osiam.addons.selfadministration.registration.service.UserValidator;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/registration")
public class Registration {

    @Inject
    private RegistrationService registrationService;

    @Inject
    private UserValidator userValidator;

    private String[] allowedFields;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(allowedFields);
    }
    
    @Value("${org.osiam.html.form.usernameEqualsEmail}")
    private boolean usernameEqualsEmail;
    
    @Value("#{'${org.osiam.html.form.fields}'.split(',')}") 
    private void setAllowedFields(List<String> allowedFields){
        if(!allowedFields.contains("email")){
            allowedFields.add("email");
        }
        if(!allowedFields.contains("password")){
            allowedFields.add("password");
        }
        if(!usernameEqualsEmail && !allowedFields.contains("userName")){
            allowedFields.add("userName");
        }
        this.allowedFields = allowedFields.toArray(new String[allowedFields.size()]);
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getRegistrationForm(Model model) {
        model.addAttribute("registrationUser", new RegistrationUser());
        model.addAttribute("allowedFields", allowedFields);
        return "registration";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String register(@Valid @ModelAttribute final RegistrationUser registrationUser,
            final BindingResult bindingResult, final Model model) {

        userValidator.validate(registrationUser, bindingResult);
        if (bindingResult.hasErrors()) {
            return "registration";
        }

        User user = registrationService.saveRegistrationUser(registrationUser);
        model.addAttribute("user", user);
        return "registrationSuccess";
    }

}
