/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.jdbc.user;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * An AuthenticatedUser that has an associated remote host.
 *
 * @author Michael Jumper 
 */
public abstract class RemoteAuthenticatedUser implements AuthenticatedUser {

    /**
     * The credentials given when this user authenticated.
     */
    private final Credentials credentials;

    /**
     * The AuthenticationProvider that authenticated this user.
     */
    private final AuthenticationProvider authenticationProvider;

    /**
     * The host from which this user authenticated.
     */
    private final String remoteHost;

    /**
     * Regular expression which matches any IPv4 address.
     */
    private static final String IPV4_ADDRESS_REGEX = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})";

    /**
     * Regular expression which matches any IPv6 address.
     */
    private static final String IPV6_ADDRESS_REGEX = "([0-9a-fA-F]*(:[0-9a-fA-F]*){0,7})";

    /**
     * Regular expression which matches any IP address, regardless of version.
     */
    private static final String IP_ADDRESS_REGEX = "(" + IPV4_ADDRESS_REGEX + "|" + IPV6_ADDRESS_REGEX + ")";

    /**
     * Pattern which matches valid values of the de-facto standard
     * "X-Forwarded-For" header.
     */
    private static final Pattern X_FORWARDED_FOR = Pattern.compile("^" + IP_ADDRESS_REGEX + "(, " + IP_ADDRESS_REGEX + ")*$");

    /**
     * Derives the remote host of the authenticating user from the given
     * credentials object. The remote host is derived from X-Forwarded-For
     * in addition to the actual source IP of the request, and thus is not
     * trusted. The derived remote host is really only useful for logging,
     * unless the server is configured such that X-Forwarded-For is guaranteed
     * to be trustworthy.
     *
     * @param credentials
     *     The credentials to derive the remote host from.
     *
     * @return
     *     The remote host from which the user with the given credentials is
     *     authenticating.
     */
    private static String getRemoteHost(Credentials credentials) {

        HttpServletRequest request = credentials.getRequest();

        // Use X-Forwarded-For, if present and valid
        String header = request.getHeader("X-Forwarded-For");
        if (header != null) {
            Matcher matcher = X_FORWARDED_FOR.matcher(header);
            if (matcher.matches())
                return matcher.group(1);
        }

        // If header absent or invalid, just use source IP
        return request.getRemoteAddr();

    }
    
    /**
     * Creates a new RemoteAuthenticatedUser, deriving the associated remote
     * host from the given credentials.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider that has authenticated the given user.
     *
     * @param credentials 
     *     The credentials given by the user when they authenticated.
     */
    public RemoteAuthenticatedUser(AuthenticationProvider authenticationProvider,
            Credentials credentials) {
        this.authenticationProvider = authenticationProvider;
        this.credentials = credentials;
        this.remoteHost = getRemoteHost(credentials);
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the host from which this user authenticated.
     *
     * @return
     *     The host from which this user authenticated.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

}
