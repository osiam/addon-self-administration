package org.osiam.addons.selfadministration.registration;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.osiam.resources.scim.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/registration")
public class RegistrationController {

    @Inject
    private RegistrationService registrationService;

    @Inject
    private UserValidator userValidator;
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(registrationService.getAllAllowedFields());
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getRegistrationForm(final Model model) {
        model.addAttribute("registrationUser", new RegistrationUser());
        model.addAttribute("allowedFields", registrationService.getAllAllowedFields());
        return "registration";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String register(@Valid @ModelAttribute final RegistrationUser registrationUser,
            final BindingResult bindingResult, final Model model, final HttpServletRequest request, final HttpServletResponse response) {

        userValidator.validate(registrationUser, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("allowedFields", registrationService.getAllAllowedFields());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "registration";
        }

        User user = registrationService.saveRegistrationUser(registrationUser);
        registrationService.sendRegistrationEmail(user, request);

        model.addAttribute("user", user);

        response.setStatus(HttpStatus.CREATED.value());
        return "registrationSuccess";
    }

    /**
     * Activates a previously registered user.
     * 
     * After activation E-Mail arrived the activation link will point to this URI.
     * 
     * @param userId
     *        the id of the registered user
     * @param activationToken
     *        the user's activation token, send by E-Mail
     * 
     * @return the registrationSuccess page
     */
    @RequestMapping(value = "/activation", method = RequestMethod.GET)
    public String confirmation(@RequestParam final String userId,
            @RequestParam final String activationToken, final Model model) {

        User user = registrationService.activateUser(userId, activationToken);

        model.addAttribute("user", user);
        
        return "activationSuccess";
    }
}