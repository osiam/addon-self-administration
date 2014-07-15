package org.osiam.addons.selfadministration.plugin.exception;

/**
 * Application exception indicating that post registration steps failed.
 * 
 * @author Timo Kanera, tarent solutions GmbH
 */
public class PostRegistrationFailedException extends Exception {

    private static final long serialVersionUID = -5211143904792666211L;

    /**
     * Creates a new {@link PostRegistrationFailedException} with the given message.
     * 
     * @param message
     *        the exception message
     */
    public PostRegistrationFailedException(String message) {
        super(message);
    }

    /**
     * Creates a new {@link PostRegistrationFailedException} with the given message and cause.
     * 
     * @param message
     *        the exception message
     * @param cause
     *        the cause
     */
    public PostRegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
