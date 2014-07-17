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
            accessToken = getOsiamConnector().retrieveAccessToken(getUserName(), getUserPassword(), Scope.ALL);
        }
        
        return accessToken;
    }

    private OsiamConnector getOsiamConnector() {
        if(connector == null){
            connector = new OsiamConnector.Builder()
                .setEndpoint(getOsiamEndpoint())
                .setClientId(getClientId())
                .setClientSecret(getClientSecret())
                .build();
        }
        
        return connector;
    }

    private String getProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }
    
    private String getUserName() {
        return getProperty("osiam.addon-selfadministration.plugin.user.name", "marissa");
    }

    private String getUserPassword() {
        return getProperty("osiam.addon-selfadministration.plugin.user.password", "koala");
    }
    
    private String getOsiamEndpoint() {
        return getProperty("osiam.addon-selfadministration.plugin.osiam.endpoint", "http://localhost:8080/");
    }

    private String getClientId() {
        return getProperty("osiam.addon-selfadministration.plugin.osiam.client.id", "example-client");
    }

    private String getClientSecret() {
        return getProperty("osiam.addon-selfadministration.plugin.osiam.client.secret", "secret");
    }
}
