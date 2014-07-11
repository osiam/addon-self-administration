package org.osiam.addons.selfadministration.plugin.api;

/**
 * Created by ukayan on 11/07/14.
 */
public class RegistrationFailedException extends Exception{

    public RegistrationFailedException() {
        super();
    }

    public RegistrationFailedException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistrationFailedException(String message) {
        super(message);
    }

    public RegistrationFailedException(Throwable cause) {
        super(cause);
    }

}
