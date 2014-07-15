package org.osiam.addons.selfadministration.plugin.impl;

import org.osiam.addons.selfadministration.plugin.api.CallbackPlugin;
import org.osiam.addons.selfadministration.plugin.exception.CallbackException;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.User;

/**
 * Simple Plugin implementation.
 */
public class PluginImpl implements CallbackPlugin {
    
    public void performPreRegistrationActions(User user) throws CallbackException {
        if (user.getEmails() != null) for (Email email : user.getEmails()) {
                if (!email.getValue().endsWith(".org")) {
                    throw new CallbackException("The given Email '" + email.getValue() + "' must end with .org!");
                }
            }
    }

    @Override
    public void performPostRegistrationActions(User user) throws CallbackException {
        // TODO Auto-generated method stub
        
    }
}
