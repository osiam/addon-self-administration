package org.osiam.web.registration.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.osiam.client.connector.OsiamConnector;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.scim.User;
import org.osiam.web.exception.InvalidAttributeException;
import org.osiam.web.registration.RegistrationUser;
import org.osiam.web.registration.UserConverter;
import org.osiam.web.service.ConnectorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.base.Strings;

@Controller
@RequestMapping(value = "/registration")
public class Registration {

    @Inject
    UserConverter userConverter;

    @Inject
    ConnectorBuilder connectorBuilder;

    @Value("${org.osiam.html.form.fields}")
    private String[] formFields;

    @Value("${org.osiam.html.form.usernameEqualsEmail}")
    private boolean usernameEqualsEmail;

    @RequestMapping(method = RequestMethod.GET)
    public String getRegistrationForm(Model model) {
        model.addAttribute("registrationUser", new RegistrationUser());
        return "registration";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String register(@Valid @ModelAttribute final RegistrationUser registrationUser,
            final BindingResult bindingResult, final Model model) {
        checkUsernameEmail(registrationUser);
        if (bindingResult.hasErrors()) {
            return "registration";
        }
        User user = saveRegistrationUser(registrationUser);
        model.addAttribute("user", user);
        return "registrationSuccess";
    }

    private User saveRegistrationUser(RegistrationUser registrationUser) {
        User user = userConverter.toScimUser(registrationUser);
        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        return osiamConnector.createUser(user, accessToken);
    }

    private void checkUsernameEmail(RegistrationUser registrationUser) {
        boolean isUserNameEmpty = Strings.isNullOrEmpty(registrationUser.getUserName());
        if (!usernameEqualsEmail && isUserNameEmpty) {
            throw new InvalidAttributeException("Username not set, but requested because usernameEqualsEmail=false",
                    "registration.exception.message");
        }
    }
}
