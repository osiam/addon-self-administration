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

package org.osiam.addons.selfadministration.registration;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.osiam.addons.selfadministration.plugin.api.RegistrationFailedException;
import org.osiam.addons.selfadministration.util.PluginHandler;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
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

import java.net.MalformedURLException;

@Controller
@RequestMapping(value = "/registration")
public class RegistrationController {

    @Value("${org.osiam.registration.precheck.plugin.enabled}")
    private String activatePrecheckPlugin;

    @Value("${org.osiam.registration.plugin.jar.path}")
    private String pluginJarPath;

    @Value("${org.osiam.registration.precheck.plugin.classname}")
    private String pluginClass;

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

        if( activatePrecheckPlugin.trim().equals("true")) {
            performPreRegistrationCheck(registrationUser);
        }

        User user = registrationService.saveRegistrationUser(registrationUser);
        registrationService.sendRegistrationEmail(user, request);

        model.addAttribute("user", user);

        response.setStatus(HttpStatus.CREATED.value());
        return "registrationSuccess";
    }

    private void performPreRegistrationCheck(RegistrationUser registrationUser) {
        try {
            PluginHandler pluginHandler = new PluginHandler(pluginJarPath, getClass().getClassLoader());
            pluginHandler.callPreRegCheck(pluginClass, registrationUser.getEmail());

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (RegistrationFailedException e) {
            // TODO: handle the error message and show it cleanly in error template
            throw new RuntimeException(e.getErrorMessage());
        }
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
    public String confirmation(@RequestParam("userId") final String userId,
            @RequestParam("activationToken") final String activationToken, final Model model) {

        User user = registrationService.activateUser(userId, activationToken);

        model.addAttribute("user", user);
        
        return "activationSuccess";
    }
}