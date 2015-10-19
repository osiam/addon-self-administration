/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.addons.self_administration.registration;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.osiam.addons.self_administration.Config;
import org.osiam.addons.self_administration.exception.UserNotRegisteredException;
import org.osiam.addons.self_administration.plugin_api.CallbackException;
import org.osiam.resources.scim.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/registration")
public class RegistrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private Config config;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(config.getAllowedFields());
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getRegistrationForm(final Model model) {
        model.addAttribute("registrationUser", new RegistrationUser());
        return "registration";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String register(@Valid final RegistrationUser registrationUser,
            final BindingResult bindingResult, final Model model, final HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("requiredFieldMapping", config.getAllAllowedFields());
            return "registration";
        }

        User user = userConverter.toScim(registrationUser);

        try {
            registrationService.preRegistration(user);
        } catch (CallbackException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("requiredFieldMapping", config.getAllAllowedFields());

            return "registration";
        }

        final String requestUrl = request.getRequestURL().toString();

        try {
            user = registrationService.registerUser(user, requestUrl);
        } catch (UserNotRegisteredException e) {
            model.addAttribute("requiredFieldMapping", config.getAllAllowedFields());
            model.addAttribute("errorKey", "registration.error.tryAgain");
            return "registration";
        }

        model.addAttribute("user", user);

        try {
            registrationService.postRegistration(user);
        } catch (CallbackException p) {
            LOGGER.error(
                    "An exception occurred while performing post registration actions for user with ID: "
                            + user.getId(),
                    p);
        }
        return "registrationSuccess";
    }

    /**
     * Activates a previously registered user.
     * <p>
     * After activation E-Mail arrived the activation link will point to this URI.
     *
     * @param userId
     *        the id of the registered user
     * @param activationToken
     *        the user's activation token, send by E-Mail
     * @return the registrationSuccess page
     */
    @RequestMapping(value = "/activation", method = RequestMethod.GET)
    public String confirmation(@RequestParam("userId") final String userId,
            @RequestParam("activationToken") final String activationToken, final Model model) {

        User user = registrationService.activateUser(userId, activationToken);

        model.addAttribute("user", user);

        return "activationSuccess";
    }
}
