package org.osiam.addons.self_administration.util;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * {@code ActivationToken} stores the actual token for activation and the timestamp the token has been issued in UTC.
 */
public class ActivationToken {

    private final String token;
    private final long issuedTime;

    /**
     * Parses an activation token from the SCIM extension
     * @param activationToken The activation token as stored in the extension
     * @return A new {@link ActivationToken} instance
     * @throws NumberFormatException
     */
    public static ActivationToken fromString(String activationToken) {
        final int indexOfColon = activationToken.indexOf(':');

        if (indexOfColon == -1) {
            return new ActivationToken(activationToken, Long.MAX_VALUE);
        }

        final String token = activationToken.substring(0, indexOfColon);
        final long issuedTime = Long.valueOf(activationToken.substring(indexOfColon + 1));

        return new ActivationToken(token, issuedTime);
    }

    /**
     * Creates a new {@link ActivationToken}
     */
    public ActivationToken() {
        this.token = UUID.randomUUID().toString();
        this.issuedTime = System.currentTimeMillis();
    }

    private ActivationToken(final String token, final long issuedTime) {
        this.token = token;
        this.issuedTime = issuedTime;
    }

    /**
     * Checks if this activation token is expired regarding the given timeout
     * @param timeout The timeout value
     * @param unit The unit of time of the timeout value
     * @return {@code true} if this activation token is expired, otherwise {@code false}
     */
    public boolean isExpired(long timeout, TimeUnit unit) {
        return System.currentTimeMillis() > issuedTime + unit.toMillis(timeout);
    }

    public String getToken() {
        return token;
    }

    public long getIssuedTime() {
        return issuedTime;
    }

    /**
     * Returns the compound activation token suitable for storing in the database
     * <p>
     * The compound activation token is created like this: "${token}:${issuedTime}"
     * </p>
     * @return
     */
    @Override
    public String toString() {
        return token + ":" + issuedTime;
    }
}
