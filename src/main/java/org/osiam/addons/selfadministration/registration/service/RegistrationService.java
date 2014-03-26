package org.osiam.addons.selfadministration.registration.service;

import javax.inject.Inject;

import org.osiam.addons.selfadministration.registration.RegistrationUser;
import org.osiam.addons.selfadministration.registration.UserConverter;
import org.osiam.addons.selfadministration.service.ConnectorBuilder;
import org.osiam.client.connector.OsiamConnector;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.scim.User;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    @Inject
    UserConverter userConverter;
    
    @Inject
    ConnectorBuilder connectorBuilder;
    
    public User saveRegistrationUser(RegistrationUser registrationUser) {
        User user = userConverter.toScimUser(registrationUser);
        OsiamConnector osiamConnector = connectorBuilder.createConnector();
        AccessToken accessToken = osiamConnector.retrieveAccessToken();
        return osiamConnector.createUser(user, accessToken);
    }
}
