package com.fsck.k9.autocrypt;


import java.io.IOException;
import java.io.InputStream;

import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeMessage;
import net.thunderbird.core.android.testing.RobolectricTest;
import net.thunderbird.core.common.exception.MessagingException;
import net.thunderbird.core.logging.legacy.Log;
import net.thunderbird.core.logging.testing.TestLogger;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class AutocryptHeaderParserTest extends RobolectricTest {
    AutocryptHeaderParser autocryptHeaderParser = AutocryptHeaderParser.getInstance();

    @Before
    public void setUp() throws Exception {
        Log.logger = new TestLogger();
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.getApplication().getCacheDir());
    }

    // Test cases taken from: https://github.com/mailencrypt/autocrypt/tree/master/src/tests/data

    @Test
    public void getValidAutocryptHeader__withNoHeader__shouldReturnNull() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/no_autocrypt.eml");

        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNull(autocryptHeader);
    }

    @Test
    public void getValidAutocryptHeader__withBrokenBase64__shouldReturnNull() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/rsa2048-broken-base64.eml");

        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNull(autocryptHeader);
    }

    @Test
    public void getValidAutocryptHeader__withSimpleAutocrypt() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/rsa2048-simple.eml");

        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNotNull(autocryptHeader);
        assertEquals("alice@testsuite.autocrypt.org", autocryptHeader.getAddr());
        assertEquals(0, autocryptHeader.getParameters().size());
        assertEquals(1225, autocryptHeader.getKeyData().length);
    }

    @Test
    public void getValidAutocryptHeader__withExplicitType() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/rsa2048-explicit-type.eml");

        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNotNull(autocryptHeader);
        assertEquals("alice@testsuite.autocrypt.org", autocryptHeader.getAddr());
        assertEquals(0, autocryptHeader.getParameters().size());
    }

    @Test
    public void getValidAutocryptHeader__withUnknownType__shouldReturnNull() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/unknown-type.eml");

        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNull(autocryptHeader);
    }

    @Test
    public void getValidAutocryptHeader__withUnknownCriticalHeader__shouldReturnNull() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/rsa2048-unknown-critical.eml");

        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNull(autocryptHeader);
    }

    @Test
    public void getValidAutocryptHeader__withUnknownNonCriticalHeader() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/rsa2048-unknown-non-critical.eml");

        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNotNull(autocryptHeader);
        assertEquals("alice@testsuite.autocrypt.org", autocryptHeader.getAddr());
        assertEquals(1, autocryptHeader.getParameters().size());
        assertEquals("ignore", autocryptHeader.getParameters().get("_monkey"));
    }

    @Test
    public void parseAutocryptHeader_toRawHeaderString() throws Exception {
        MimeMessage message = parseFromResource("autocrypt/rsa2048-simple.eml");
        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(message);

        assertNotNull(autocryptHeader);
        String headerValue = autocryptHeader.toRawHeaderString();
        headerValue = headerValue.substring("Autocrypt: ".length());
        AutocryptHeader parsedAutocryptHeader = autocryptHeaderParser.parseAutocryptHeader(headerValue);

        assertEquals(autocryptHeader, parsedAutocryptHeader);
    }

    private MimeMessage parseFromResource(String resourceName) throws IOException, MessagingException {
        InputStream inputStream = readFromResourceFile(resourceName);
        return MimeMessage.parseMimeMessage(inputStream, false);
    }

    private InputStream readFromResourceFile(String name) {
        return getClass().getResourceAsStream("/" + name);
    }
}
