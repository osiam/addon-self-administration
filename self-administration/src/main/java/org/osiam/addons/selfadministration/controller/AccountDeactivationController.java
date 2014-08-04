/*
 * Copyright (C) 2014 tarent AG
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
package org.osiam.addons.selfadministration.controller;

import javax.servlet.http.HttpServletRequest;

import org.osiam.addons.selfadministration.exception.OsiamException;
import org.osiam.addons.selfadministration.util.RegistrationHelper;
import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * A controller providing an operation for account deactivation.
 */
public class AccountDeactivationController extends AbstractAccountController {

    /**
     * Deactivates the user with the given ID.
     * 
     * @param authorization
     *        Authorization header with access token
     * @param userId
     *        the user ID
     * @return the resulting HTTP status code
     */
    @RequestMapping(value = "/deactivation/{userId}", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> deactivateUser(@RequestHeader("Authorization") final String authorization,
            @PathVariable final String userId, final HttpServletRequest request) {

        AccessToken accessToken = new AccessToken.Builder(RegistrationHelper.extractAccessToken(authorization)).build();

        try {
            User user = getUser(userId, accessToken);
            deactivateUser(userId, accessToken);
            sendEmail(user, "deactivation");
        } catch (OsiamClientException | OsiamException | MailException e) {
            return handleException(e);
        }

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    /*
     * Deactivates the account of the user with the given user ID.
     */
    private void deactivateUser(String userId, AccessToken token) {
        UpdateUser updateUser = getUpdateUserForDeactivation();
        getConnector().updateUser(userId, updateUser, token);
    }

    /*
     * Builds an UpdateUser for the deactivation.
     */
    private UpdateUser getUpdateUserForDeactivation() {
        return new UpdateUser.Builder().updateActive(false).build();
    }
}
