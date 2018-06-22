package com.fsck.k9.message.signature;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TextSignatureRemoverTest {
    @Test
    public void shouldStripSignature() throws Exception {
        String text = "This is the body text\r\n" +
                "\r\n" +
                "-- \r\n" +
                "Sent from my Android device with K-9 Mail. Please excuse my brevity.";

        String withoutSignature = TextSignatureRemover.stripSignature(text);

        assertEquals("This is the body text\r\n\r\n", withoutSignature);
    }
}
