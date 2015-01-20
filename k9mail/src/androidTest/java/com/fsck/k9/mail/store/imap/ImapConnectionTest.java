package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.endtoend.framework.StubMailServer;
import com.fsck.k9.endtoend.framework.UserForImap;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;


@RunWith(AndroidJUnit4.class)
public class ImapConnectionTest  {
    private static final String[] CAPABILITIES = new String[] { "IMAP4REV1", "LITERAL+", "QUOTA" };

    private StubMailServer stubMailServer;
    private ImapConnection connection;
    private TestImapSettings settings;

    @Before
    public void setUp() throws Exception {
        stubMailServer = new StubMailServer();
        settings = new TestImapSettings(UserForImap.TEST_USER);
        connection = new ImapConnection(settings, null, null);
    }

    @After
    public void tearDown() throws Exception {
        stubMailServer.stop();
    }

    @Test(expected = MessagingException.class)
    public void testOpenConnectionWithoutRunningServerThrowsMessagingException() throws Exception {
        stubMailServer.stop();
        connection.open();
    }

    @Test(expected = AuthenticationFailedException.class)
    public void testOpenConnectionWithWrongCredentialsThrowsAuthenticationFailedException() throws Exception {
        connection = new ImapConnection(new TestImapSettings("wrong", "password"), null, null);
        connection.open();
    }

    @Test
    public void testConnectionIsInitiallyClosed() throws Exception {
        assertFalse(connection.isOpen());
    }

    @Test
    public void testSuccessfulOpenConnectionTogglesOpenState() throws Exception {
        connection.open();
        assertTrue(connection.isOpen());
    }

    @Test
    public void testSuccessfulOpenAndCloseConnectionTogglesOpenState() throws Exception {
        connection.open();
        connection.close();
        assertFalse(connection.isOpen());
    }

    @Test
    public void testCapabilitiesAreInitiallyEmpty() throws Exception {
        assertTrue(connection.getCapabilities().isEmpty());
    }

    @Test
    public void testCapabilitiesListGetsParsedCorrectly() throws Exception {
        connection.open();
        List<String> capabilities = new ArrayList<String>(connection.getCapabilities());
        Collections.sort(capabilities);
        assertArrayEquals(CAPABILITIES, capabilities.toArray());
    }

    @Test
    public void testHasCapabilityChecks() throws Exception {
        connection.open();
        for (String capability : CAPABILITIES) {
            assertTrue(connection.hasCapability(capability));
        }
        assertFalse(connection.hasCapability("FROBAZIFCATE"));
    }

    @Test
    public void testPathPrefixGetsSetCorrectly() throws Exception {
        connection.open();
        assertEquals("", settings.getPathPrefix());
    }

    @Test
    public void testPathDelimiterGetsParsedCorrectly() throws Exception {
        connection.open();
        assertEquals(".", settings.getPathDelimiter());
    }

    @Test
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
