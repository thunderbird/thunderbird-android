package com.fsck.k9.mail.helpers;


import java.io.IOException;
import java.io.OutputStream;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import okio.BufferedSink;
import okio.Okio;


class TestMessage extends MimeMessage {
    private final long messageSize;
    private final Address[] from;
    private final Address[] to;
    private final boolean hasAttachments;


    TestMessage(TestMessageBuilder builder) {
        from = toAddressArray(builder.from);
        to = toAddressArray(builder.to);
        hasAttachments = builder.hasAttachments;
        messageSize = builder.messageSize;
    }

    @Override
    public Address[] getFrom() {
        return from;
    }

    @Override
    public Address[] getRecipients(RecipientType type) {
        switch (type) {
            case TO:
                return to;
            case CC:
                return new Address[0];
            case BCC:
                return new Address[0];
        }

        throw new AssertionError("Missing switch case: " + type);
    }

    @Override
    public boolean hasAttachments() {
        return hasAttachments;
    }

    @Override
    public long calculateSize() {
        return messageSize;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        BufferedSink bufferedSink = Okio.buffer(Okio.sink(out));
        bufferedSink.writeUtf8("[message data]");
        bufferedSink.emit();
    }

    private static Address[] toAddressArray(String email) {
        return email == null ? new Address[0] : new Address[] { new Address(email) };
    }
}
