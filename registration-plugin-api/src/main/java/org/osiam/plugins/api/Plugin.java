package org.osiam.plugins.api;

/**
 * Plugin interface.
 */
public interface Plugin {

	/**
	 * Performs the plugin action.
	 */
	public void performPreRegistrationCheck(String name) throws RegistrationFailedException;

}

