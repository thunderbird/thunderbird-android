package com.fsck.k9.mail.internet;


import java.io.IOException;

import com.fsck.k9.mail.MessagingException;
import okio.Buffer;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TextBodyTest {
    @Test
    public void getSize_withSignUnsafeData_shouldReturnCorrectValue() throws Exception {
        TextBody textBody = new TextBody("From Bernd");
        textBody.setEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);

        long result = textBody.getSize();
        
        int outputSize = getSizeOfSerializedBody(textBody);
        assertEquals(outputSize, result);
    }

    private int getSizeOfSerializedBody(TextBody textBody) throws IOException, MessagingException {
        Buffer buffer = new Buffer();
        textBody.writeTo(buffer.outputStream());
        return buffer.readByteString().size();
    }
}
