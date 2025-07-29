package org.dcache.nfs.v4;

import java.net.InetSocketAddress;

public interface ConnectionAuthenticator {
    /**
     * Dummy authenticator, permitting all connections.
     */
    static ConnectionAuthenticator DUMMY_AUTHENTICATOR = new ConnectionAuthenticator() {

        @Override
        public boolean requiresConnectionAuthentication(InetSocketAddress addr) {
            return false;
        }

        @Override
        public void notifyNewConnection(InetSocketAddress addr, Runnable terminate) {
        }

        @Override
        public void notifyConnectionClosed(InetSocketAddress addr) {
        }

        @Override
        public void setAuthenticated(InetSocketAddress addr, boolean verified) {
        }

        @Override
        public boolean mayUseSecondaryPortForVerification(InetSocketAddress client, InetSocketAddress secondary) {
            return false; // no need
        }
    };

    /**
     * Returns {@code true} if the given socket address (still) needs to authenticate.
     * 
     * @param addr The socket address.
     * @return {@code true} if authentication is (still) necessary.
     */
    boolean requiresConnectionAuthentication(InetSocketAddress addr);

    /**
     * Marks the given socket address as authenticated (or not).
     * 
     * @param addr The socket address.
     * @param verified Whether the address is authenticated.
     */
    void setAuthenticated(InetSocketAddress addr, boolean verified);

    /**
     * Notifies the authenticator that a new connection from the given address has been established.
     * 
     * @param addr The remote socket address.
     * @param terminate A callback that can be used to forcibly terminate the connection.
     */
    void notifyNewConnection(InetSocketAddress addr, Runnable terminate);

    /**
     * Notifies the authenticator that the connection from the given address has been closed.
     * 
     * @param addr The remote socket address.
     */
    void notifyConnectionClosed(InetSocketAddress addr);

    /**
     * Checks if we may use the given secondary address (usually pointing to an NFSv4 callback) for the purpose of
     * verifying the authentication state of the given client address.
     * 
     * @param client The client address.
     * @param secondary The secondary address.
     * @return {@code true} if verification may be attempted.
     */
    boolean mayUseSecondaryPortForVerification(InetSocketAddress client, InetSocketAddress secondary);
}
