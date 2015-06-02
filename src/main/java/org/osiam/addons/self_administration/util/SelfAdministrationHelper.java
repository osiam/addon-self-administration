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

import java.util.Locale;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.base.Strings;

public class SelfAdministrationHelper {

    private SelfAdministrationHelper() {
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

    public static ResponseEntity<String> createErrorResponseEntity(String message, HttpStatus httpStatus) {
        return new ResponseEntity<>("{\"error\":\"" + message + "\"}", httpStatus);
    }

    public static Duration makeDuration(String value) {
        PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
                .appendDays().appendSuffix("d").appendSeparator(" ")
                .appendHours().appendSuffix("h").appendSeparator(" ")
                .appendMinutes().appendSuffix("m").appendSeparator(" ")
                .appendSeconds().appendSuffix("s")
                .toFormatter();

        return periodFormatter.parsePeriod(value.trim()).toStandardDuration();
    }
}
