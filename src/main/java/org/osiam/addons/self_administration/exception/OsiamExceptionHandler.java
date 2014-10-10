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

package org.osiam.addons.self_administration.exception;

import javax.servlet.http.HttpServletResponse;

import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.OsiamRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class OsiamExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsiamExceptionHandler.class.getName());
    private static final String AN_EXCEPTION_OCCURED = "An exception occurred";
    private static final String KEY = "key";
    private ModelAndView modelAndView = new ModelAndView("self_administration_error");

    @ExceptionHandler(OsiamRequestException.class)
    protected ModelAndView handleException(OsiamRequestException ex, HttpServletResponse response) {
        LOGGER.warn(AN_EXCEPTION_OCCURED, ex);
        return createResponse(response, ex.getHttpStatusCode());
    }

    @ExceptionHandler(OsiamClientException.class)
    protected ModelAndView handleConflict(OsiamClientException ex, HttpServletResponse response) {
        LOGGER.error(AN_EXCEPTION_OCCURED, ex);
        return createResponse(response, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(OsiamException.class)
    protected ModelAndView handleException(OsiamException ex, HttpServletResponse response) {
        LOGGER.warn(AN_EXCEPTION_OCCURED, ex);
        return createResponse(response, ex.getHttpStatusCode());
    }

    private ModelAndView createResponse(HttpServletResponse response, int httpStatus) {
        response.setStatus(httpStatus);
        modelAndView.addObject(KEY, "registration.form.error");
        return modelAndView;
    }
}
