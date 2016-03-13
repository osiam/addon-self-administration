/*
* Copyright (C) 2015 tarent AG
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

package org.osiam.addons.self_administration.plugin.impl;

import org.osiam.addons.self_administration.plugin_api.CallbackException;
import org.osiam.addons.self_administration.plugin_api.CallbackPlugin;
import org.osiam.client.OsiamConnector;
import org.osiam.client.oauth.AccessToken;
import org.osiam.client.oauth.Scope;
import org.osiam.client.query.Query;
import org.osiam.client.query.QueryBuilder;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Group;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.UpdateGroup;
import org.osiam.resources.scim.User;

/**
 * Simple Plugin implementation.
 */
public class PluginImpl implements CallbackPlugin {

    private static final String TEST_GROUP_NAME = "Test";

    private OsiamConnector connector;
    private AccessToken accessToken;

    public void performPreRegistrationActions(User user) throws CallbackException {
        if (user.getEmails() != null) for (Email email : user.getEmails()) {
            if (!email.getValue().endsWith(".org")) {
                throw new CallbackException("The given Email '" + email.getValue() + "' must end with .org!");
            }
        }
    }

    @Override
    public void performPostRegistrationActions(User user) throws CallbackException {
        OsiamConnector connector = getOsiamConnector();

        Group testGroup = getTestGroup(connector);
        UpdateGroup uGroup = new UpdateGroup.Builder().addMember(user.getId()).build();

        connector.updateGroup(testGroup.getId(), uGroup, getAccessToken());
    }

    private Group getTestGroup(OsiamConnector connector) {
        Query groupQuery = new QueryBuilder().filter("displayName eq \"" + TEST_GROUP_NAME + "\"").build();
        SCIMSearchResult<Group> result = connector.searchGroups(groupQuery, getAccessToken());

        if (result.getTotalResults() > 0) {
            Group testGroup = result.getResources().get(0);
            return testGroup;
        }

        return createTestGroup(connector);
    }

    private Group createTestGroup(OsiamConnector connector) {
        Group testGroup = new Group.Builder(TEST_GROUP_NAME).build();

        return connector.createGroup(testGroup, getAccessToken());
    }

    private AccessToken getAccessToken() {
        if (accessToken == null) {
            accessToken = getOsiamConnector().retrieveAccessToken(getUserName(), getUserPassword(), Scope.ADMIN);
        }

        return accessToken;
    }

    private OsiamConnector getOsiamConnector() {
        if (connector == null) {
            OsiamConnector.Builder builder = new OsiamConnector.Builder()
                    .setClientId(getClientId())
                    .setClientSecret(getClientSecret());

            builder.withEndpoint(getOsiamEndpoint());

            connector = builder.build();
        }

        return connector;
    }

    private String getUserName() {
        return System.getProperty("osiam.addon-self-administration.plugin.user.name", "admin");
    }

    private String getUserPassword() {
        return System.getProperty("osiam.addon-self-administration.plugin.user.password", "koala");
    }

    private String getOsiamEndpoint() {
        return System.getProperty("osiam.addon-self-administration.plugin.osiam.endpoint", "http://localhost:8080/osiam");
    }

    private String getOsiamVersion() {
        return System.getProperty("osiam.addon-self-administration.plugin.osiam.version", "3");
    }

    private String getClientId() {
        return System.getProperty("osiam.addon-self-administration.plugin.osiam.client.id", "example-client");
    }

    private String getClientSecret() {
        return System.getProperty("osiam.addon-self-administration.plugin.osiam.client.secret", "secret");
    }
}
