package org.osiam.addons.self_administration.plugin;

import org.osiam.addons.self_administration.plugin_api.CallbackException;
import org.osiam.addons.self_administration.plugin_api.CallbackPlugin;
import org.osiam.resources.scim.User;

public class NoopPlugin implements CallbackPlugin {
    
    @Override
    public void performPreRegistrationActions(final User user) throws CallbackException {
        // no-op
    }

    @Override
    public void performPostRegistrationActions(final User user) throws CallbackException {
        // no-op
    }
}
