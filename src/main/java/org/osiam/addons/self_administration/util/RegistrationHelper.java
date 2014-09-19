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

package org.osiam.addons.self_administration.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.osiam.resources.scim.Email;
import org.springframework.context.i18n.LocaleContextHolder;

import com.google.common.base.Strings;

public class RegistrationHelper {

    private RegistrationHelper() {
    }

    public static List<Email> replaceOldPrimaryMail(String newEmail, List<Email> emails) {

        List<Email> updatedEmailList = new ArrayList<>();

        // add new primary email address
        updatedEmailList.add(new Email.Builder()
                .setValue(newEmail)
                .setPrimary(true)
                .build());

        // add only non primary mails to new list and remove all primary entries
        for (Email mail : emails) {
            if (mail.isPrimary()) {
                updatedEmailList.add(new Email.Builder().setType(mail.getType())
                        .setPrimary(mail.isPrimary())
                        .setValue(mail.getValue()).setOperation("delete").build());
            } else {
                updatedEmailList.add(mail);
            }
        }

        return updatedEmailList;
    }

    public static String createLinkForEmail(String linkPrefix, String userId, String parameterName, String parameter) {
        StringBuilder link = new StringBuilder(linkPrefix).append("?");
        link.append("userId=").append(userId);
        link.append("&").append(parameterName).append("=");
        return link.append(parameter).toString();
    }

    public static String extractAccessToken(String authorizationHeader) {
        int lastIndexOf = authorizationHeader.lastIndexOf(' ');
        return authorizationHeader.substring(lastIndexOf + 1);
    }

    public static Locale getLocale(String userLocale) {
        if (!Strings.isNullOrEmpty(userLocale)) {
            Locale locale = new Locale(userLocale);
            if (!locale.toLanguageTag().equals("und")) { // Undetermined
                return locale;
            }
        }
        return LocaleContextHolder.getLocale();
    }
}
