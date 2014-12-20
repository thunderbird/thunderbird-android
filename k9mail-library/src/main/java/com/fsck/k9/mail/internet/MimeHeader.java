
package com.fsck.k9.mail.internet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.*;

public class MimeHeader {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Application specific header that contains Store specific information about an attachment.
     * In IMAP this contains the IMAP BODYSTRUCTURE part id so that the ImapStore can later
     * retrieve the attachment at will from the server.
     * The info is recorded from this header on LocalStore.appendMessages and is put back
     * into the MIME data by LocalStore.fetch.
     */
    public static final String HEADER_ANDROID_ATTACHMENT_STORE_DATA = "X-Android-Attachment-StoreData";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_ID = "Content-ID";

    /**
     * Fields that should be omitted when writing the header using writeTo()
     */
    private static final String[] writeOmitFields = {
//        HEADER_ANDROID_ATTACHMENT_DOWNLOADED,
//        HEADER_ANDROID_ATTACHMENT_ID,
        HEADER_ANDROID_ATTACHMENT_STORE_DATA
    };

    private List<Field> mFields = new ArrayList<Field>();
    private String mCharset = null;

    public void clear() {
        mFields.clear();
    }

    public String getFirstHeader(String name) {
        String[] header = getHeader(name);
        if (header == null) {
            return null;
        }
        return header[0];
    }

    public void addHeader(String name, String value) {
        mFields.add(new Field(name, MimeUtility.foldAndEncode(value)));
    }

    public void setHeader(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        removeHeader(name);
        addHeader(name, value);
    }

    public Set<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<String>();
        for (Field field : mFields) {
            names.add(field.name);
        }
        return names;
    }

    public String[] getHeader(String name) {
        List<String> values = new ArrayList<String>();
        for (Field field : mFields) {
            if (field.name.equalsIgnoreCase(name)) {
                values.add(field.value);
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.toArray(EMPTY_STRING_ARRAY);
    }

    public void removeHeader(String name) {
        List<Field> removeFields = new ArrayList<Field>();
        for (Field field : mFields) {
            if (field.name.equalsIgnoreCase(name)) {
                removeFields.add(field);
            }
        }
        mFields.removeAll(removeFields);
    }

    public void writeTo(OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        for (Field field : mFields) {
            if (!Arrays.asList(writeOmitFields).contains(field.name)) {
                String v = field.value;

                if (hasToBeEncoded(v)) {
                    Charset charset = null;

                    if (mCharset != null) {
                        charset = Charset.forName(mCharset);
                    }
                    v = EncoderUtil.encodeEncodedWord(field.value, charset);
                }

                writer.write(field.name);
                writer.write(": ");
                writer.write(v);
                writer.write("\r\n");
            }
        }
        writer.flush();
    }

    // encode non printable characters except LF/CR/TAB codes.
    private boolean hasToBeEncoded(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c < 0x20 || 0x7e < c) && // non printable
                    (c != 0x0a && c != 0x0d && c != 0x09)) { // non LF/CR/TAB
                return true;
            }
        }

        return false;
    }

    private static class Field {
        private final String name;

        private final String value;

        public Field(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("(");
            sb.append(name).append('=').append(value).append(')');
            return sb.toString();
        }
    }

    public void setCharset(String charset) {
        mCharset = charset;
    }

    @Override
    public MimeHeader clone() {
        MimeHeader header = new MimeHeader();
        header.mCharset = mCharset;

        header.mFields = new ArrayList<Field>(mFields);

        return header;
    }
}
