package org.osiam.addons.selfadministration.plugin.impl;

import org.osiam.resources.scim.User;
import org.osiam.addons.selfadministration.plugin.api.Plugin;
import org.osiam.addons.selfadministration.plugin.api.RegistrationFailedException;

/**
 * Simple Plugin implementation.
 *
 */
public class PluginImpl implements Plugin {
    
    public void performPreRegistrationCheck(User user) throws RegistrationFailedException{
        throw new RegistrationFailedException("Email must end with .com");
    }

}
