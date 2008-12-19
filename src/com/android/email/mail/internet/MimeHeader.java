
package com.android.email.mail.internet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import com.android.email.Utility;
import com.android.email.mail.MessagingException;

public class MimeHeader {
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

    /**
     * Fields that should be omitted when writing the header using writeTo()
     */
    private static final String[] writeOmitFields = {
//        HEADER_ANDROID_ATTACHMENT_DOWNLOADED,
//        HEADER_ANDROID_ATTACHMENT_ID,
        HEADER_ANDROID_ATTACHMENT_STORE_DATA
    };

    protected ArrayList<Field> mFields = new ArrayList<Field>();

    public void clear() {
        mFields.clear();
    }

    public String getFirstHeader(String name) throws MessagingException {
        String[] header = getHeader(name);
        if (header == null) {
            return null;
        }
        return header[0];
    }

    public void addHeader(String name, String value) throws MessagingException {
        mFields.add(new Field(name, MimeUtility.foldAndEncode(value)));
    }

    public void setHeader(String name, String value) throws MessagingException {
        if (name == null || value == null) {
            return;
        }
        removeHeader(name);
        addHeader(name, value);
    }

    public String[] getHeader(String name) throws MessagingException {
        ArrayList<String> values = new ArrayList<String>();
        for (Field field : mFields) {
            if (field.name.equalsIgnoreCase(name)) {
                values.add(field.value);
            }
        }
        if (values.size() == 0) {
            return null;
        }
        return values.toArray(new String[] {});
    }

    public void removeHeader(String name) throws MessagingException {
        ArrayList<Field> removeFields = new ArrayList<Field>();
        for (Field field : mFields) {
            if (field.name.equalsIgnoreCase(name)) {
                removeFields.add(field);
            }
        }
        mFields.removeAll(removeFields);
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        for (Field field : mFields) {
            if (!Utility.arrayContains(writeOmitFields, field.name)) {
                writer.write(field.name + ": " + field.value + "\r\n");
            }
        }
        writer.flush();
    }

    class Field {
        String name;

        String value;

        public Field(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
