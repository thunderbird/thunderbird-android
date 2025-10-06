
package com.fsck.k9.mail.internet;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fsck.k9.mail.Body;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mail.filter.CountingOutputStream;
import com.fsck.k9.mail.filter.SignSafeOutputStream;

import org.apache.james.mime4j.Charsets;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;
import org.jetbrains.annotations.Nullable;


public class TextBody implements Body, SizeAware {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];


    private final String text;
    private String encoding;
    // Length of the message composed (as opposed to quoted). I don't like the name of this variable and am open to
    // suggestions as to what it should otherwise be. -achen 20101207
    @Nullable
    private Integer composedMessageLength;
    // Offset from position 0 where the composed message begins.
    @Nullable
    private Integer composedMessageOffset;

    public TextBody(String body) {
        this.text = body;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        if (text != null) {
            byte[] bytes = text.getBytes(Charsets.UTF_8);
            if (MimeUtil.ENC_QUOTED_PRINTABLE.equalsIgnoreCase(encoding)) {
                writeSignSafeQuotedPrintable(out, bytes);
            } else if (MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)) {
                out.write(bytes);
            } else {
                throw new IllegalStateException("Cannot get size for encoding!");
            }
        }
    }

    public String getRawText() {
        return text;
    }

    @Override
    public InputStream getInputStream() throws MessagingException {
        byte[] b;
        if (text != null) {
            b = text.getBytes(Charsets.UTF_8);
        } else {
            b = EMPTY_BYTE_ARRAY;
        }
        return new ByteArrayInputStream(b);
    }

    @Override
    public void setEncoding(String encoding) {
        boolean isSupportedEncoding = MimeUtil.ENC_QUOTED_PRINTABLE.equalsIgnoreCase(encoding) ||
                MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding);
        if (!isSupportedEncoding) {
            throw new IllegalArgumentException("Cannot encode to " + encoding);
        }

        this.encoding = encoding;
    }

    @Nullable
    public Integer getComposedMessageLength() {
        return composedMessageLength;
    }

    public void setComposedMessageLength(@Nullable Integer composedMessageLength) {
        this.composedMessageLength = composedMessageLength;
    }

    @Nullable
    public Integer getComposedMessageOffset() {
        return composedMessageOffset;
    }

    public void setComposedMessageOffset(@Nullable Integer composedMessageOffset) {
        this.composedMessageOffset = composedMessageOffset;
    }

    @Override
    public long getSize() {
        try {
            byte[] bytes = text.getBytes(Charsets.UTF_8);

            if (MimeUtil.ENC_QUOTED_PRINTABLE.equalsIgnoreCase(encoding)) {
                return getLengthWhenQuotedPrintableEncoded(bytes);
            } else if (MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)) {
                return bytes.length;
            } else {
                throw new IllegalStateException("Cannot get size for encoding!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't get body size", e);
        }
    }

    private long getLengthWhenQuotedPrintableEncoded(byte[] bytes) throws IOException {
        try (CountingOutputStream countingOutputStream = new CountingOutputStream()) {
            writeSignSafeQuotedPrintable(countingOutputStream, bytes);
            return countingOutputStream.getCount();
        }
    }

    private void writeSignSafeQuotedPrintable(OutputStream out, byte[] bytes) throws IOException {
        try (SignSafeOutputStream signSafeOutputStream = new SignSafeOutputStream(out)) {
            try (QuotedPrintableOutputStream signSafeQuotedPrintableOutputStream = new QuotedPrintableOutputStream(
                    signSafeOutputStream, false)) {
                signSafeQuotedPrintableOutputStream.write(bytes);
            }
        }
    }

    public String getEncoding() {
        return encoding;
    }
}
