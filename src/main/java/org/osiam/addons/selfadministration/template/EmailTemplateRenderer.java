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

package org.osiam.addons.selfadministration.template;

import java.util.Map;

import org.osiam.addons.selfadministration.exception.TemplateException;
import org.osiam.resources.scim.User;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.fragment.DOMSelectorFragmentSpec;
import org.thymeleaf.spring4.SpringTemplateEngine;

import com.google.common.base.Strings;

/**
 * Email template renderer service for thymeleaf template engine
 * 
 */
@Service
public class EmailTemplateRenderer {

    public String renderEmailSubject(String templateName, User user, Map<String, String> variables) {
        String emailSubject = renderTemplate(templateName, "#mail-subject", user, variables);
        if (Strings.isNullOrEmpty(emailSubject)) {
            throw new TemplateException(
                    "Could not find the mail subject in your template file '" + templateName
                            + "'. Please provide an HTML element with the ID 'mail-subject'.", "template.email.exception.malformed");
        }
        return emailSubject;
    }
    
    public String renderEmailBody(String templateName, User user, Map<String, String> variables) {
        String emailBody = renderTemplate(templateName, "#mail-body", user, variables);
        if (Strings.isNullOrEmpty(emailBody)) {
            throw new TemplateException(
                    "Could not find the mail body in your template file '" + templateName
                            + "'. Please provide an HTML element with the ID 'mail-body'.", "template.email.exception.malformed");
        }
        return emailBody;
    }
    
    private String renderTemplate(String templateName, String selectorExpression, User user, Map<String, String> variables) {
        final Context context = new Context();
        context.setVariable("user", user);
        context.setVariables(variables);
        
        OsiamClasspathTemplateResolver emailTemplateResolver = initializeTemplateResolver(user.getLocale());
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver);
        
        return templateEngine.process(templateName, context, new DOMSelectorFragmentSpec(selectorExpression));
    }
    
    private OsiamClasspathTemplateResolver initializeTemplateResolver(String locale) {
        OsiamClasspathTemplateResolver emailTemplateResolver = new OsiamClasspathTemplateResolver(locale);
        emailTemplateResolver.setPrefix("addon-self-administration/templates/mail/");
        emailTemplateResolver.setTemplateMode("HTML5");
        emailTemplateResolver.setCharacterEncoding("UTF-8");
        emailTemplateResolver.setOrder(1);
        return emailTemplateResolver;
    }
}