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

package org.osiam.addons.self_administration.service;

import org.osiam.client.OsiamConnector;
import org.osiam.client.oauth.Scope;
import org.osiam.client.query.Query;
import org.osiam.client.query.QueryBuilder;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OsiamService {

    @Autowired
    private OsiamConnector osiamConnector;

    public boolean isUsernameIsAlreadyTaken(String userName) {
        Query query = new QueryBuilder().filter("userName eq \"" + userName + "\"").build();

        SCIMSearchResult<User> queryResult = osiamConnector.searchUsers(query,
                osiamConnector.retrieveAccessToken(Scope.ADMIN));
        return queryResult.getTotalResults() != 0L;
    }

    public User createUser(User user) {
        return osiamConnector.createUser(user, osiamConnector.retrieveAccessToken(Scope.ADMIN));
    }

    public User getUser(String userId) {
        return osiamConnector.getUser(userId, osiamConnector.retrieveAccessToken(Scope.ADMIN));
    }

    public User updateUser(String userId, UpdateUser updateUser) {
        return osiamConnector.updateUser(userId, updateUser, osiamConnector.retrieveAccessToken(Scope.ADMIN));
    }

    public void deleteUser(String id) {
        osiamConnector.deleteUser(id, osiamConnector.retrieveAccessToken(Scope.ADMIN));
    }
}
