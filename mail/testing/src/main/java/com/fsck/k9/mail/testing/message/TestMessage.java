package com.fsck.k9.mail.testing.message;


import java.io.IOException;
import java.io.OutputStream;

import com.fsck.k9.mail.Address;
import net.thunderbird.core.common.exception.MessagingException;
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
            case BCC:
            case X_ORIGINAL_TO:
            case DELIVERED_TO:
            case X_ENVELOPE_TO:
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

    private static Address[] toAddressArray(String[] emails) {
        return emails == null ? new Address[0] : stringArrayToAddressArray(emails);
    }

    private static Address[] stringArrayToAddressArray(String[] emails) {
        Address addresses[] = new Address[emails.length];
        for (int i = 0; i < emails.length; i++) {
            addresses[i] = new Address(emails[i]);
        }
        return addresses;
    }
}
