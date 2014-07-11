package org.osiam.plugins.impl;

import org.osiam.plugins.api.Plugin;
import org.osiam.plugins.api.RegistrationFailedException;

/**
 * Simple Plugin implementation.
 *
 */
public class PluginImpl implements Plugin
{
    public void performPreRegistrationCheck(String name) throws RegistrationFailedException{

	    if ( !name.endsWith("com")){
            throw new RegistrationFailedException("Email must end with .com");
        }

    }

}
