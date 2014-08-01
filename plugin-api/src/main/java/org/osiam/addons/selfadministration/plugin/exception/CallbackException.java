package org.osiam.addons.selfadministration.plugin.exception;

/**
 * Application exception indicating that callback steps failed.
 */
public class CallbackException extends Exception {

    private static final long serialVersionUID = -5211143904792666211L;

    /**
     * Creates a new {@link CallbackException} with the given message.
     * 
     * @param message
     *        the exception message
     */
    public CallbackException(String message) {
        super(message);
    }

    /**
     * Creates a new {@link CallbackException} with the given message and cause.
     * 
     * @param message
     *        the exception message
     * @param cause
     *        the cause
     */
    public CallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
