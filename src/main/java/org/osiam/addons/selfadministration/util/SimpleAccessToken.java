/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.addons.selfadministration.util;

import java.util.Objects;

import org.osiam.client.oauth.AccessToken;

public class SimpleAccessToken extends AccessToken {

    public SimpleAccessToken(String token) {
        this.token = token;
    }
    
    /**
     * Retrieve the string value of the access token used to authenticate against the provider.
     * @return The access token string
     */
    @Override
    public String getToken() {
        return token;
    }

    /**
     * use the basic class {@link AccessToken} to retrieve type
     * @exception UnsupportedOperationException
     */
    @Override
    public String getType() {
        throw new UnsupportedOperationException();
    }

    /**
     * use the basic class {@link AccessToken} to retrieve experiseIn
     * @exception UnsupportedOperationException
     */
    @Override
    public int getExpiresIn() {
        throw new UnsupportedOperationException();
    }

    /**
     * use the basic class {@link AccessToken} to retrieve expired
     * @exception UnsupportedOperationException
     */
    @Override
    public boolean isExpired() {
        throw new UnsupportedOperationException();
    }

    /**
     * use the basic class {@link AccessToken} to retrieve scope
     * @exception UnsupportedOperationException
     */
    @Override
    public String getScope() {
        throw new UnsupportedOperationException();
    }

    /**
     * use the basic class {@link AccessToken} to retrieve refreshToken
     * @exception UnsupportedOperationException
     */
    @Override
    public String getRefreshToken() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AccessToken that = (AccessToken) o;

        return token.equals(that.getToken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.token);
    }
}
