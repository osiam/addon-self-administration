package org.osiam.addons.selfadministration.plugin.impl;

import org.osiam.addons.selfadministration.plugin.api.CallbackPlugin;
import org.osiam.addons.selfadministration.plugin.exception.CallbackException;
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

    private static final String OSIAM_ENDPOINT = "http://localhost:8080/";
    private static final String CLIENT_ID = "example-client";
    private static final String CLIENT_SECRET = "secret";

    private static final String USER_NAME = "marissa";
    private static final String USER_PASSWORD = "koala";
    
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

        if(result.getTotalResults() > 0){
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
        if(accessToken == null){
            accessToken = getOsiamConnector().retrieveAccessToken(USER_NAME, USER_PASSWORD, Scope.ALL);
        }
        
        return accessToken;
    }

    private OsiamConnector getOsiamConnector() {
        if(connector == null){
            connector = new OsiamConnector.Builder()
                .setEndpoint(OSIAM_ENDPOINT)
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .build();
        }
        
        return connector;
    }
}
