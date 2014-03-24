package org.osiam.web.registration.controller;

import java.util.Map;

import org.osiam.web.registration.RegistrationUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String register(Model model) {
        Map<String, Object> modelMap = model.asMap();
        return "registrationSuccess";
    }
}
