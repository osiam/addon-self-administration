package org.osiam.addons.selfadministration.plugin.impl;

import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.User;
import org.osiam.addons.selfadministration.plugin.api.CallbackPlugin;
import org.osiam.addons.selfadministration.plugin.exception.PostRegistrationFailedException;
import org.osiam.addons.selfadministration.plugin.exception.PreRegistrationFailedException;

/**
 * Simple Plugin implementation.
 *
 */
public class PluginImpl implements CallbackPlugin {
    
    public void performPreRegistrationActions(User user) throws PreRegistrationFailedException{
        if(user.getEmails() != null) for(Email email : user.getEmails()){
            
            if(!email.getValue().endsWith(".org")){
                throw new PreRegistrationFailedException("The given Email '" + email.getValue() + "' must end with .org!");
            }
        }
    }

    @Override
    public void performPostRegistrationActions(User user) throws PostRegistrationFailedException {
        // TODO Auto-generated method stub
        
    }
}
