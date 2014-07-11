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

}

