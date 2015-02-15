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

import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletResponse;

import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.OsiamRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class OsiamExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsiamExceptionHandler.class.getName());
    private static final String AN_EXCEPTION_OCCURRED = "An exception occurred";
    private static final String KEY = "key";
    private ModelAndView modelAndView = new ModelAndView("web/self_administration_error");

    @ExceptionHandler(OsiamRequestException.class)
    protected ModelAndView handleException(OsiamRequestException ex, HttpServletResponse response) {
        response.setStatus(ex.getHttpStatusCode());
        LOGGER.warn(AN_EXCEPTION_OCCURRED, ex);
        modelAndView.addObject(KEY, "registration.form.error");
        return modelAndView;
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(OsiamClientException.class)
    protected ModelAndView handleConflict(OsiamClientException ex) {
        LOGGER.error(AN_EXCEPTION_OCCURRED, ex);
        return createResponse("registration.form.error");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(OsiamException.class)
    protected ModelAndView handleException(OsiamException ex) {
        LOGGER.error(AN_EXCEPTION_OCCURRED, ex);
        return createResponse(ex.getKey());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidAttributeException.class)
    protected ModelAndView handleException(InvalidAttributeException ex) {
        LOGGER.warn(AN_EXCEPTION_OCCURRED, ex);
        return createResponse(ex.getKey());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NoSuchElementException.class)
    protected ModelAndView handleNoSuchElementException(NoSuchElementException ex) {
        LOGGER.warn(AN_EXCEPTION_OCCURRED, ex);
        return createResponse("activation.exception");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    protected ModelAndView handleException(Exception ex) {
        LOGGER.error(AN_EXCEPTION_OCCURRED, ex);
        return createResponse("internal.server.error");
    }

    private ModelAndView createResponse(String messageKey) {
        modelAndView.addObject(KEY, messageKey);
        return modelAndView;
    }
}
