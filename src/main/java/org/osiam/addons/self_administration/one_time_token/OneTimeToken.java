package org.osiam.addons.self_administration.one_time_token;

import java.util.UUID;

/**
 * {@code ActivationToken} stores the actual token for activation and the timestamp the token has been issued in UTC.
 */
public class OneTimeToken {

    private final String token;
    private final long issuedTime;

    /**
     * Parses an activation token from the SCIM extension
     * @param activationToken The activation token as stored in the extension
     * @return A new {@link OneTimeToken} instance
     * @throws NumberFormatException
     */
    public static OneTimeToken fromString(String activationToken) {
        final int indexOfColon = activationToken.indexOf(':');

        if (indexOfColon == -1) {
            return new OneTimeToken(activationToken, Long.MAX_VALUE);
        }

        final String token = activationToken.substring(0, indexOfColon);
        final long issuedTime = Long.valueOf(activationToken.substring(indexOfColon + 1));

        return new OneTimeToken(token, issuedTime);
    }

    /**
     * Creates a new {@link OneTimeToken}
     */
    public OneTimeToken() {
        this.token = UUID.randomUUID().toString();
        this.issuedTime = System.currentTimeMillis();
    }

    private OneTimeToken(final String token, final long issuedTime) {
        this.token = token;
        this.issuedTime = issuedTime;
    }

    /**
     * Checks if this activation token is expired regarding the given timeout
     * @param timeout The timeout in milliseconds as a long
     * @return {@code true} if this activation token is expired, otherwise {@code false}
     */
    public boolean isExpired(long timeout) {
        return System.currentTimeMillis() - timeout > issuedTime;
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
