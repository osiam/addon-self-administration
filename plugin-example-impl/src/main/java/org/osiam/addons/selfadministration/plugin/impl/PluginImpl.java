package org.osiam.addons.selfadministration.plugin.impl;

import org.osiam.addons.selfadministration.plugin.api.Plugin;
import org.osiam.addons.selfadministration.plugin.api.RegistrationFailedException;

/**
 * Simple Plugin implementation.
 *
 */
public class PluginImpl implements Plugin {
    
    public void performPreRegistrationCheck(String name) throws RegistrationFailedException{

	    if ( !name.endsWith("com")){
            throw new RegistrationFailedException("Email must end with .com");
        }

    }

}
