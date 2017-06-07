package com.fsck.k9.mail.filter;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.ByteArrayOutputStream;


import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class SmtpDataStuffingTest {
    @Test
    public void TestSmtpDotStuffing() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        EOLConvertingOutputStream eolConvertingOutputStream = new EOLConvertingOutputStream(
                new LineWrapOutputStream(new SmtpDataStuffing(byteArrayOutputStream), 1000));

        String messageAfterStuffing = "Hello dot\r\n..";

        // sampleDotText = "Hello dot\n."
        byte[] sampleDotText = {
                72,
                101,
                108,
                108,
                111,
                32,
                100,
                111,
                116,
                10,
                46
        };

        try {
            eolConvertingOutputStream.write(sampleDotText, 0, 11);
        }
        finally {
            eolConvertingOutputStream.close();
            byteArrayOutputStream.close();
        }

        String stuffedMessage = new String(byteArrayOutputStream.toByteArray());
        assertEquals(stuffedMessage, messageAfterStuffing);
    }
}
