package org.osiam.addons.selfadministration.plugin.api;

import org.osiam.resources.scim.User;

/**
 * Plugin interface.
 */
public interface Plugin {

    /**
     * Performs the plugin action.
     */
    public void performPreRegistrationCheck(User user) throws RegistrationFailedException;

    /**
     * Performs post registration actions for the given {@link User}.
     * 
     * @param user
     *        the newly registered user
     * @throws PostRegistrationFailedException
     *         if the post registration steps failed
     */
    public void performPostRegistrationActions(User user) throws PostRegistrationFailedException;
}
