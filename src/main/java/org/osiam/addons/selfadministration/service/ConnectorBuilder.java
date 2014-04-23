package org.osiam.addons.selfadministration.service;

import org.osiam.client.connector.OsiamConnector;
import org.osiam.client.oauth.GrantType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConnectorBuilder {

    @Value("${org.osiam.resource-server.home}")
    private String resourceServerHome;

    @Value("${org.osiam.auth-server.home}")
    private String authServerHome;

    @Value("${org.osiam.addon-self-administration.client.id}")
    private String clientId;
    
    @Value("${org.osiam.addon-self-administration.client.secret}")
    private String clientSecret;

    @Value("${org.osiam.addon-self-administration.client.scope}")
    private String clientScope;

    public OsiamConnector createConnector(String userName, String password) {
        OsiamConnector.Builder oConBuilder = new OsiamConnector.Builder().
                setAuthServerEndpoint(authServerHome).
                setResourceServerEndpoint(resourceServerHome).
                setClientId(clientId).
                setClientSecret(clientSecret).
                setGrantType(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS).
                setUserName(userName).
                setPassword(password).
                setScope(clientScope);
        return oConBuilder.build();
    }

    public OsiamConnector createConnector() {
        OsiamConnector.Builder oConBuilder = new OsiamConnector.Builder().
                setAuthServerEndpoint(authServerHome).
                setResourceServerEndpoint(resourceServerHome).
                setGrantType(GrantType.CLIENT_CREDENTIALS).
                setClientId(clientId).
                setClientSecret(clientSecret).
                setScope(clientScope);
        return oConBuilder.build();
    }
}
