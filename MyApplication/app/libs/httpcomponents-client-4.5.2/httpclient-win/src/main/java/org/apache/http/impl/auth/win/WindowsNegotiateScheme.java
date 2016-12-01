/*
 * ====================================================================
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.impl.auth.win;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;

import com.sun.jna.platform.win32.Secur32;
import com.sun.jna.platform.win32.Sspi;
import com.sun.jna.platform.win32.Sspi.CredHandle;
import com.sun.jna.platform.win32.Sspi.CtxtHandle;
import com.sun.jna.platform.win32.Sspi.SecBufferDesc;
import com.sun.jna.platform.win32.Sspi.TimeStamp;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.ptr.IntByReference;

/**
 * Auth scheme that makes use of JNA to implement Negotiate and NTLM on Windows Platforms.
 * <p>
 * This will delegate negotiation to the windows machine.
 * </p>
 * <p>
 * EXPERIMENTAL
 * </p>
 *
 * @since 4.4
 */
@NotThreadSafe
public class WindowsNegotiateScheme extends AuthSchemeBase {

    private final Log log = LogFactory.getLog(getClass());

    // NTLM or Negotiate
    private final String scheme;
    private final String servicePrincipalName;

    private CredHandle clientCred;
    private CtxtHandle sspiContext;
    private boolean continueNeeded;
    private String challenge;

    public WindowsNegotiateScheme(final String scheme, final String servicePrincipalName) {
        super();

        this.scheme = (scheme == null) ? AuthSchemes.SPNEGO : scheme;
        this.challenge = null;
        this.continueNeeded = true;
        this.servicePrincipalName = servicePrincipalName;

        if (this.log.isDebugEnabled()) {
            this.log.debug("Created WindowsNegotiateScheme using " + this.scheme);
        }
    }

    public void dispose() {
        if (clientCred != null && !clientCred.isNull()) {
            final int rc = Secur32.INSTANCE.FreeCredentialsHandle(clientCred);
            if (WinError.SEC_E_OK != rc) {
                throw new Win32Exception(rc);
            }
        }
        if (sspiContext != null && !sspiContext.isNull()) {
            final int rc = Secur32.INSTANCE.DeleteSecurityContext(sspiContext);
            if (WinError.SEC_E_OK != rc) {
                throw new Win32Exception(rc);
            }
        }
        continueNeeded = true; // waiting
        clientCred = null;
        sspiContext = null;
    }

    @Override
    public void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    @Override
    public String getSchemeName() {
        return scheme;
    }

    // String parameters not supported
    @Override
    public String getParameter(final String name) {
        return null;
    }

    // NTLM/Negotiate do not support authentication realms
    @Override
    public String getRealm() {
        return null;
    }

    @Override
    public boolean isConnectionBased() {
        return true;
    }

    @Override
    protected void parseChallenge(
            final CharArrayBuffer buffer,
            final int beginIndex,
            final int endIndex) throws MalformedChallengeException {
        this.challenge = buffer.substringTrimmed(beginIndex, endIndex);

        if (this.challenge.isEmpty()) {
            if (clientCred != null) {
                dispose(); // run cleanup first before throwing an exception otherwise can leak OS resources
                if (continueNeeded) {
                    throw new RuntimeException("Unexpected token");
                }
            }
        }
    }

    @Override
    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request,
            final HttpContext context) throws AuthenticationException {

        final String response;
        if (clientCred == null) {
            // ?? We don't use the credentials, should we allow anything?
            if (!(credentials instanceof CurrentWindowsCredentials)) {
                throw new InvalidCredentialsException(
                        "Credentials cannot be used for " + getSchemeName() + " authentication: "
                                + credentials.getClass().getName());
            }

            // client credentials handle
            try {
                final String username = CurrentWindowsCredentials.getCurrentUsername();
                final TimeStamp lifetime = new TimeStamp();

                clientCred = new CredHandle();
                final int rc = Secur32.INSTANCE.AcquireCredentialsHandle(username,
                        scheme, Sspi.SECPKG_CRED_OUTBOUND, null, null, null, null,
                        clientCred, lifetime);

                if (WinError.SEC_E_OK != rc) {
                    throw new Win32Exception(rc);
                }

                final String targetName = getServicePrincipalName(context);
                response = getToken(null, null, targetName);
            } catch (final RuntimeException ex) {
                failAuthCleanup();
                if (ex instanceof Win32Exception) {
                    throw new AuthenticationException("Authentication Failed", ex);
                } else {
                    throw ex;
                }
            }
        } else if (this.challenge == null || this.challenge.isEmpty()) {
            failAuthCleanup();
            throw new AuthenticationException("Authentication Failed");
        } else {
            try {
                final byte[] continueTokenBytes = Base64.decodeBase64(this.challenge);
                final SecBufferDesc continueTokenBuffer = new SecBufferDesc(
                        Sspi.SECBUFFER_TOKEN, continueTokenBytes);
                final String targetName = getServicePrincipalName(context);
                response = getToken(this.sspiContext, continueTokenBuffer, targetName);
            } catch (final RuntimeException ex) {
                failAuthCleanup();
                if (ex instanceof Win32Exception) {
                    throw new AuthenticationException("Authentication Failed", ex);
                } else {
                    throw ex;
                }
            }
        }

        final CharArrayBuffer buffer = new CharArrayBuffer(scheme.length() + 30);
        if (isProxy()) {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
            buffer.append(AUTH.WWW_AUTH_RESP);
        }
        buffer.append(": ");
        buffer.append(scheme); // NTLM or Negotiate
        buffer.append(" ");
        buffer.append(response);
        return new BufferedHeader(buffer);
    }

    private void failAuthCleanup() {
        dispose();
        this.continueNeeded = false;
    }

    // Per RFC4559, the Service Principal Name should HTTP/<hostname>. However, <hostname>
    // can just be the host or the fully qualified name (e.g., see "Kerberos SPN generation"
    // at http://www.chromium.org/developers/design-documents/http-authentication). Here,
    // I've chosen to use the host that has been provided in HttpHost so that I don't incur
    // any additional DNS lookup cost.
    private String getServicePrincipalName(final HttpContext context) {
        final String spn;
        if (this.servicePrincipalName != null) {
            spn = this.servicePrincipalName;
        } else {
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final HttpHost target = clientContext.getTargetHost();
            if (target != null) {
                spn = "HTTP/" + target.getHostName();
            } else {
                final RouteInfo route = clientContext.getHttpRoute();
                if (route != null) {
                    spn = "HTTP/" + route.getTargetHost().getHostName();
                } else {
                    // Should not happen
                    spn = null;
                }
            }
        }
        if (this.log.isDebugEnabled()) {
            this.log.debug("Using SPN: " + spn);
        }
        return spn;
    }

    // See http://msdn.microsoft.com/en-us/library/windows/desktop/aa375506(v=vs.85).aspx
    String getToken(
            final CtxtHandle continueCtx,
            final SecBufferDesc continueToken,
            final String targetName) {
        final IntByReference attr = new IntByReference();
        final SecBufferDesc token = new SecBufferDesc(
                Sspi.SECBUFFER_TOKEN, Sspi.MAX_TOKEN_SIZE);

        sspiContext = new CtxtHandle();
        final int rc = Secur32.INSTANCE.InitializeSecurityContext(clientCred,
                continueCtx, targetName, Sspi.ISC_REQ_DELEGATE | Sspi.ISC_REQ_MUTUAL_AUTH, 0,
                Sspi.SECURITY_NATIVE_DREP, continueToken, 0, sspiContext, token,
                attr, null);
        switch (rc) {
            case WinError.SEC_I_CONTINUE_NEEDED:
                continueNeeded = true;
                break;
            case WinError.SEC_E_OK:
                dispose(); // Don't keep the context
                continueNeeded = false;
                break;
            default:
                dispose();
                throw new Win32Exception(rc);
        }
        return Base64.encodeBase64String(token.getBytes());
    }

    @Override
    public boolean isComplete() {
        return !continueNeeded;
    }

    @Override
    @Deprecated
    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request) throws AuthenticationException {
        return authenticate(credentials, request, null);
    }

}
