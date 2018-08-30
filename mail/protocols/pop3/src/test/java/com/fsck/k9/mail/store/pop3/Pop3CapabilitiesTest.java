package com.fsck.k9.mail.store.pop3;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class Pop3CapabilitiesTest {

    @Test
    public void toString_producesReadableOutput() {
        String result = new Pop3Capabilities().toString();

        assertEquals(
                "CRAM-MD5 false, PLAIN false, STLS false, TOP false, UIDL false, EXTERNAL false",
                result);
    }
}
