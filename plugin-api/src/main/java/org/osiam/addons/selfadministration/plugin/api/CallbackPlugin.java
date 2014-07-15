package org.osiam.addons.selfadministration.plugin.api;

import org.osiam.addons.selfadministration.plugin.exception.PostRegistrationFailedException;
import org.osiam.addons.selfadministration.plugin.exception.PreRegistrationFailedException;
import org.osiam.resources.scim.User;

/**
 * Defines callback operations to be performed by an external plug-in.
 */
public interface CallbackPlugin {

    /**
     * Performs pre-registration actions for the given {@link User}.
     * 
     * @param user
     *        the new user
     * @throws PreRegistrationFailedException
     *         if the pre-registration checks failed
     */
    public void performPreRegistrationActions(User user) throws PreRegistrationFailedException;

    /**
     * Performs post-registration actions for the given {@link User}.
     * 
     * @param user
     *        the newly registered user
     * @throws PostRegistrationFailedException
     *         if the post-registration steps failed
     */
    public void performPostRegistrationActions(User user) throws PostRegistrationFailedException;
}
