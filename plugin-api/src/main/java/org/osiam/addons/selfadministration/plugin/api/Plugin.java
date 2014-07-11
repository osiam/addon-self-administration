package org.osiam.addons.selfadministration.plugin.api;

/**
 * Plugin interface.
 */
public interface Plugin {

	/**
	 * Performs the plugin action.
	 */
	public void performPreRegistrationCheck(String name) throws RegistrationFailedException;

}

