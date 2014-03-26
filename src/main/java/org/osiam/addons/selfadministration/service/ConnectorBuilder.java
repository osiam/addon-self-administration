package org.osiam.addons.selfadministration.service;

import org.osiam.client.connector.OsiamConnector;
import org.osiam.client.oauth.GrantType;
import org.osiam.client.oauth.Scope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConnectorBuilder {

    @Value("${osiam.server.port}")
    private int serverPort;
    @Value("${osiam.server.host}")
    private String serverHost;
    @Value("${osiam.server.http.scheme}")
    private String httpScheme;
    @Value("${org.osiam.auth.client.id}")
    private String clientId;
    @Value("${org.osiam.auth.client.secret}")
    private String clientSecret;
    @Value("${org.osiam.auth.client.scope}")
    private String clientScope;

    public OsiamConnector createConnector(String userName, String password) {
        OsiamConnector.Builder oConBuilder = new OsiamConnector.Builder().
                setAuthServiceEndpoint(buildServerBaseUri("osiam-auth-server")).
                setResourceEndpoint(buildServerBaseUri("osiam-resource-server")).
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
                setAuthServiceEndpoint(buildServerBaseUri("osiam-auth-server")).
                setResourceEndpoint(buildServerBaseUri("osiam-resource-server")).
                setGrantType(GrantType.CLIENT_CREDENTIALS).
                setClientId(clientId).
                setClientSecret(clientSecret).
                setScope(clientScope);
        return oConBuilder.build();
    }

    private String buildServerBaseUri(String endpoint) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append(httpScheme)
                .append("://")
                .append(serverHost)
                .append(":")
                .append(serverPort)
                .append("/")
                .append(endpoint);

        return stringBuilder.toString();
    }
}
