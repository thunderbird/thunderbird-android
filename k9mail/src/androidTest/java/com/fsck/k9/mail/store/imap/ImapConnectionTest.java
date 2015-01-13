package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.endtoend.framework.StubMailServer;
import com.fsck.k9.endtoend.framework.UserForImap;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.ConnectionSecurity;
import junit.framework.TestCase;

import static org.junit.Assert.assertArrayEquals;


public class ImapConnectionTest extends TestCase {
    private static final String[] CAPABILITIES = new String[] { "IMAP4REV1", "LITERAL+", "QUOTA" };
    
    private StubMailServer stubMailServer;
    private ImapConnection connection;
    private TestImapSettings settings = new TestImapSettings(UserForImap.TEST_USER);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        stubMailServer = new StubMailServer();
        connection = new ImapConnection(settings, null, null);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        stubMailServer.stop();
    }

    public void testOpenConnectionWithWrongCredentialsThrowsAuthenticationFailedException() throws Exception {
        connection = new ImapConnection(new TestImapSettings("wrong", "password"), null, null);
        try {
            connection.open();
            fail("expected exception");
        } catch (AuthenticationFailedException e) {
            assertTrue(e.getMessage().contains("Invalid login/password"));
            assertFalse(connection.isOpen());
        }
    }

    public void testSuccessfulOpenConnectionTogglesOpenState() throws Exception {
        connection.open();
        assertTrue(connection.isOpen());
    }

    public void testSuccessfulOpenAndCloseConnectionTogglesOpenState() throws Exception {
        connection.open();
        connection.close();
        assertFalse(connection.isOpen());
    }

    public void testCapabilitiesAreInitiallyEmpty() throws Exception {
        assertTrue(connection.getCapabilities().isEmpty());
    }

    public void testCapabilitiesListGetsParsedCorrectly() throws Exception {
        connection.open();
        List<String> capabilities = new ArrayList<String>(connection.getCapabilities());
        Collections.sort(capabilities);
        assertArrayEquals(CAPABILITIES, capabilities.toArray());
    }

    public void testHasCapabilityChecks() throws Exception {
        connection.open();
        for (String capability : CAPABILITIES) {
            assertTrue(connection.hasCapability(capability));
        }
        assertFalse(connection.hasCapability("FROBAZIFCATE"));
    }

    public void testPathPrefixGetsSetCorrectly() throws Exception {
        connection.open();
        assertEquals("", settings.getPathPrefix());
    }

    public void testPathDelimiterGetsParsedCorrectly() throws Exception {
        connection.open();
        assertEquals(".", settings.getPathDelimiter());
    }

    public void testCombinedPrefixGetsSetCorrectly() throws Exception {
        connection.open();
        assertNull(settings.getCombinedPrefix());
    }

    private class TestImapSettings implements ImapSettings {
        private String pathPrefix;
        private String pathDelimiter;
        private String username;
        private String password;
        private String combinedPrefix;

        public TestImapSettings(UserForImap userForImap) {
            this(userForImap.loginUsername, userForImap.password);
        }

        public TestImapSettings(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public String getHost() {
            return stubMailServer.getImapBindAddress();
        }

        @Override
        public int getPort() {
            return stubMailServer.getImapPort();
        }

        @Override
        public ConnectionSecurity getConnectionSecurity() {
            return ConnectionSecurity.NONE;
        }

        @Override
        public AuthType getAuthType() {
            return AuthType.PLAIN;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getClientCertificateAlias() {
            return null;
        }

        @Override
        public boolean useCompression(int type) {
            return false;
        }

        @Override
        public String getPathPrefix() {
            return pathPrefix;
        }

        @Override
        public void setPathPrefix(String prefix) {
            pathPrefix = prefix;
        }

        @Override
        public String getPathDelimiter() {
            return pathDelimiter;
        }

        @Override
        public void setPathDelimiter(String delimiter) {
            pathDelimiter = delimiter;
        }

        @Override
        public String getCombinedPrefix() {
            return combinedPrefix;
        }

        @Override
        public void setCombinedPrefix(String prefix) {
            combinedPrefix = prefix;
        }
    }
}
