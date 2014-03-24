package org.osiam.web.registration.controller;

import javax.validation.Valid;

import org.osiam.web.registration.RegistrationUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/registration")
public class Registration {

    @RequestMapping(method = RequestMethod.GET)
    public String getRegistrationForm(Model model) {
        model.addAttribute("registrationUser", new RegistrationUser());
        return "registration";
    }
    
    @RequestMapping(method = RequestMethod.POST)
    public String register(@Valid @ModelAttribute final RegistrationUser registrationUser,
            final BindingResult bindingResult, final Model model) {
        if (bindingResult.hasErrors()) {
            return "registration";
        }
        model.addAttribute("user", registrationUser);
        return "registrationSuccess";
    }
}
