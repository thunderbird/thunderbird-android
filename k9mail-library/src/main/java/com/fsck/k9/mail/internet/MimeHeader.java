
package com.fsck.k9.mail.internet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.*;

public class MimeHeader {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_ID = "Content-ID";


    private List<Field> mFields = new ArrayList<Field>();
    private String mCharset = null;

    public void clear() {
        mFields.clear();
    }

    public String getFirstHeader(String name) {
        String[] header = getHeader(name);
        if (header.length == 0) {
            return null;
        }
        return header[0];
    }

    public void addHeader(String name, String value) {
        Field field = Field.newNameValueField(name, MimeUtility.foldAndEncode(value));
        mFields.add(field);
    }

    void addRawHeader(String name, String raw) {
        Field field = Field.newRawField(name, raw);
        mFields.add(field);
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
            names.add(field.getName());
        }
        return names;
    }

    public String[] getHeader(String name) {
        List<String> values = new ArrayList<String>();
        for (Field field : mFields) {
            if (field.getName().equalsIgnoreCase(name)) {
                values.add(field.getValue());
            }
        }
        return values.toArray(EMPTY_STRING_ARRAY);
    }

    public void removeHeader(String name) {
        List<Field> removeFields = new ArrayList<Field>();
        for (Field field : mFields) {
            if (field.getName().equalsIgnoreCase(name)) {
                removeFields.add(field);
            }
        }
        mFields.removeAll(removeFields);
    }

    public void writeTo(OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        for (Field field : mFields) {
            if (field.hasRawData()) {
                writer.write(field.getRaw());
            } else {
                writeNameValueField(writer, field);
            }
            writer.write("\r\n");
        }
        writer.flush();
    }

    private void writeNameValueField(BufferedWriter writer, Field field) throws IOException {
        String value = field.getValue();

        if (hasToBeEncoded(value)) {
            Charset charset = null;

            if (mCharset != null) {
                charset = Charset.forName(mCharset);
            }
            value = EncoderUtil.encodeEncodedWord(field.getValue(), charset);
        }

        writer.write(field.getName());
        writer.write(": ");
        writer.write(value);
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
        private final String raw;

        public static Field newNameValueField(String name, String value) {
            if (value == null) {
                throw new NullPointerException("Argument 'value' cannot be null");
            }

            return new Field(name, value, null);
        }

        public static Field newRawField(String name, String raw) {
            if (raw == null) {
                throw new NullPointerException("Argument 'raw' cannot be null");
            }
            if (name != null && !raw.startsWith(name + ":")) {
                throw new IllegalArgumentException("The value of 'raw' needs to start with the supplied field name " +
                        "followed by a colon");
            }

            return new Field(name, null, raw);
        }

        private Field(String name, String value, String raw) {
            if (name == null) {
                throw new NullPointerException("Argument 'name' cannot be null");
            }

            this.name = name;
            this.value = value;
            this.raw = raw;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            if (value != null) {
                return value;
            }

            int delimiterIndex = raw.indexOf(':');
            if (delimiterIndex == raw.length() - 1) {
                return "";
            }

            return raw.substring(delimiterIndex + 1).trim();
        }

        public String getRaw() {
            return raw;
        }

        public boolean hasRawData() {
            return raw != null;
        }

        @Override
        public String toString() {
            return (hasRawData()) ? getRaw() : getName() + ": " + getValue();
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
