package org.osiam.addons.selfadministration.plugin.exception;

/**
 * Created by ukayan on 11/07/14.
 */
public class PreRegistrationFailedException extends Exception{

    public PreRegistrationFailedException() {
        super();
    }

    public PreRegistrationFailedException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PreRegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreRegistrationFailedException(String message) {
        super(message);
    }

    public PreRegistrationFailedException(Throwable cause) {
        super(cause);
    }

}
