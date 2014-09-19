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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.exception.OsiamRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class OsiamExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(OsiamExceptionHandler.class.getName());
    private static final String AN_EXCEPTION_OCCURED = "An exception occurred";
    private static final String KEY = "key";
    private ModelAndView modelAndView = new ModelAndView("self_administration_error");
    
    @ExceptionHandler(OsiamRequestException.class)
    protected ModelAndView handleException(OsiamRequestException ex, HttpServletResponse response) {
        LOGGER.log(Level.WARNING, AN_EXCEPTION_OCCURED, ex);
        response.setStatus(ex.getHttpStatusCode());
        modelAndView.addObject(KEY, "registration.form.error");
        setLoggingInformation(ex);
        return modelAndView;
    }
    
    @ExceptionHandler(OsiamClientException.class)
    protected ModelAndView handleConflict(OsiamClientException ex, HttpServletResponse response) {
        LOGGER.log(Level.WARNING, AN_EXCEPTION_OCCURED, ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        modelAndView.addObject(KEY, "registration.form.error");
        setLoggingInformation(ex);
        return modelAndView;
    }

    @ExceptionHandler(OsiamException.class)
    protected ModelAndView handleException(OsiamException ex, HttpServletResponse response) {
        LOGGER.log(Level.WARNING, AN_EXCEPTION_OCCURED, ex);
        response.setStatus(ex.getHttpStatusCode());
        modelAndView.addObject(KEY, ex.getKey());
        setLoggingInformation(ex);
        return modelAndView;
    }

    @ExceptionHandler(RuntimeException.class)
    protected ModelAndView handleException(RuntimeException ex, HttpServletResponse response) {
        LOGGER.log(Level.WARNING, AN_EXCEPTION_OCCURED, ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        modelAndView.addObject(KEY, "registration.exception.message");
        setLoggingInformation(ex);
        return modelAndView;
    }

    private void setLoggingInformation(Throwable t){
        Level level = getLogLevel(LOGGER);
        if(level != null && level.intValue() <= Level.INFO.intValue()){
            modelAndView.addObject("exception", t);
        }
    }
    
    private Level getLogLevel(Logger logger){
        Level level = null;
        if(logger != null){
            level = logger.getLevel();
        }
        if(level == null){
            level = getLogLevel(logger.getParent());
        }
        return level;
    }
}