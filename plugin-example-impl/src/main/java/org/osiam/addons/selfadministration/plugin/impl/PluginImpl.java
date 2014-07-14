package org.osiam.addons.selfadministration.plugin.impl;

import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.User;
import org.osiam.addons.selfadministration.plugin.api.Plugin;
import org.osiam.addons.selfadministration.plugin.api.RegistrationFailedException;

/**
 * Simple Plugin implementation.
 *
 */
public class PluginImpl implements Plugin {
    
    public void performPreRegistrationCheck(User user) throws RegistrationFailedException{
        if(user.getEmails() != null) for(Email email : user.getEmails()){
            
            if(!email.getValue().endsWith(".org")){
                throw new RegistrationFailedException("The given Email '" + email.getValue() + "' must end with .org!");
            }
        }
    }

}
