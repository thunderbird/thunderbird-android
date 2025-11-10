package com.fsck.k9.autocrypt;


import com.fsck.k9.mail.internet.MimeMessage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertArrayEquals;


public class AutocryptOperationsHelper {

    public static void assertMessageHasAutocryptHeader(
            MimeMessage message, String addr, boolean isPreferEncryptMutual, byte[] keyData) {
        AutocryptHeader autocryptHeader = AutocryptHeaderParser.INSTANCE.getValidAutocryptHeader(message);

        assertNotNull(autocryptHeader);
        assertEquals(addr, autocryptHeader.getAddr());
        assertEquals(isPreferEncryptMutual, autocryptHeader.isPreferEncryptMutual());
        assertArrayEquals(keyData, autocryptHeader.getKeyData());
    }
}
