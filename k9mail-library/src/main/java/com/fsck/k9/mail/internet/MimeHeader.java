
package com.fsck.k9.mail.internet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.*;

public class MimeHeader implements Cloneable {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_ID = "Content-ID";


    private List<Field> mFields = new ArrayList<Field>();
    private String mCharset = null;

    private ContentType contentType;

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
        if (name.equalsIgnoreCase(HEADER_CONTENT_TYPE)){
            contentType = null;
        }
    }

    void addRawHeader(String name, String raw) {
        Field field = Field.newRawField(name, raw);
        mFields.add(field);
        if (name.equalsIgnoreCase(HEADER_CONTENT_TYPE)){
            contentType = null;
        }
    }

    public void setHeader(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        removeHeader(name);
        addHeader(name, value);
        if (name.equalsIgnoreCase(HEADER_CONTENT_TYPE)){
            contentType = null;
        }
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
        if (name.equalsIgnoreCase(HEADER_CONTENT_TYPE)){
            contentType = null;
        }
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

    public String getContentTypeParameter(String attribute) {
        parseContentType();
        return contentType.getParameter(attribute);
    }

    public void addContentTypeParameter(String attribute, String value) {
        parseContentType();
        contentType.addContentTypeParameter(attribute, value);
        setHeader(HEADER_CONTENT_TYPE, contentType.toString());
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
        try {
            MimeHeader header = (MimeHeader)super.clone();
            header.mFields = new ArrayList<Field>(mFields);
            return header;
        } catch(CloneNotSupportedException e){
            throw new RuntimeException(e);
        }
    }

    private ContentType parseContentType(){
        if (contentType == null){
            contentType = new ContentType(getFirstHeader(HEADER_CONTENT_TYPE));
        }
        return contentType;
    }

    public String getType() {
        return parseContentType().getType();
    }

    public String getSubtype() {
        return parseContentType().getSubtype();
    }

    public String getTypeAndSubtype(){
        return getType() + "/" + getSubtype();
    }
}

class ContentType {
    private String type;
    private String subtype;
    private LinkedHashMap<String, String> contentTypeParameters = new LinkedHashMap<String, String>();

    public ContentType(String value) {
        if (value == null){
            type = null;
            subtype = null;
        } else {
            value = value.replaceAll("\r|\n", "");
            String[] params = value.split(";");
            try {
                contentTypeParameters.clear();
                String[] type = params[0].trim().split("/");
                this.type = type[0];
                subtype = type[1];
                for (int i = 1; i < params.length; i++) {
                    String[] attributeValue = params[i].split("=");
                    addContentTypeParameter(attributeValue[0].trim(), attributeValue[1].trim());
                }
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("content type format : 'type/subtype:att1=value1;att2=\"value2\"'");
            }
        }
    }

    public void addContentTypeParameter(String attribute, String value) {
        if (type == null) {
            type = "text";
            subtype = "plain";
            contentTypeParameters.put("charset", "us-ascii");
        }
        contentTypeParameters.put(attribute, value);
    }

    public String toString() {
        if (type == null){
            return null;
        } else {
            StringBuilder out = new StringBuilder();
            out.append(type);
            out.append("/");
            out.append(subtype);

            int lineLength = type.length() + subtype.length() + MimeHeader.HEADER_CONTENT_TYPE.length() + 2;
            for (Map.Entry<String, String> parameterPair : contentTypeParameters.entrySet()) {
                String parameter = parameterPair.getKey() + "=" + parameterPair.getValue();
                /* some MTA truncate Content-Type at 128 for security purpose */
                if (lineLength + parameter.length() + 2 > 128) {
                    out.append("\r\n  ");
                    lineLength = 2;
                }
                out.append("; ");
                out.append(parameter);
                lineLength += parameter.length() + 2;
            }
            return out.toString();
        }
    }

    public String getParameter(String attribute) {
        String result;
        if (type == null) {
            if ("charset".equalsIgnoreCase(attribute)){
                result = "us-ascii";
            } else {
                result = null;
            }
        } else {
            result = contentTypeParameters.get(attribute);
        }
        if (result != null && result.startsWith("\"") && result.endsWith("\"")){
            result = result.substring(1, result.length()-1);
        }
        return result;
    }

    public String getType(){
        return type;
    }

    public String getSubtype(){
        return subtype;
    }
}
