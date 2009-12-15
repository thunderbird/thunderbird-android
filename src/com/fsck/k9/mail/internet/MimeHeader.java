
package com.fsck.k9.mail.internet;

import com.fsck.k9.Utility;
import com.fsck.k9.mail.MessagingException;
import org.apache.james.mime4j.codec.EncoderUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MimeHeader
{
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
    private static final String[] writeOmitFields =
    {
//        HEADER_ANDROID_ATTACHMENT_DOWNLOADED,
//        HEADER_ANDROID_ATTACHMENT_ID,
        HEADER_ANDROID_ATTACHMENT_STORE_DATA
    };

    protected ArrayList<Field> mFields = new ArrayList<Field>();

    public void clear()
    {
        mFields.clear();
    }

    public String getFirstHeader(String name)
    {
        String[] header = getHeader(name);
        if (header == null)
        {
            return null;
        }
        return header[0];
    }

    public void addHeader(String name, String value)
    {
        mFields.add(new Field(name, MimeUtility.foldAndEncode(value)));
    }

    public void setHeader(String name, String value)
    {
        if (name == null || value == null)
        {
            return;
        }
        removeHeader(name);
        addHeader(name, value);
    }

    public List<String> getHeaderNames()
    {
        ArrayList<String> names = new ArrayList<String>();
        for (Field field : mFields)
        {
            names.add(field.name);
        }
        return names;
    }

    public String[] getHeader(String name)
    {
        ArrayList<String> values = new ArrayList<String>();
        for (Field field : mFields)
        {
            if (field.name.equalsIgnoreCase(name))
            {
                values.add(field.value);
            }
        }
        if (values.size() == 0)
        {
            return null;
        }
        return values.toArray(new String[] {});
    }

    public void removeHeader(String name)
    {
        ArrayList<Field> removeFields = new ArrayList<Field>();
        for (Field field : mFields)
        {
            if (field.name.equalsIgnoreCase(name))
            {
                removeFields.add(field);
            }
        }
        mFields.removeAll(removeFields);
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException
    {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        for (Field field : mFields)
        {
            if (!Utility.arrayContains(writeOmitFields, field.name))
            {
                String v = field.value;

                if (hasToBeEncoded(v))
                {
                    v = EncoderUtil.encodeEncodedWord(
                            field.value,
                            EncoderUtil.Usage.WORD_ENTITY
                        );
                }

                writer.write(field.name + ": " + v + "\r\n");
            }
        }
        writer.flush();
    }

    // encode non printable characters except LF/CR codes.
    public boolean hasToBeEncoded(String text)
    {
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c < 0x20 || 0x7e < c)   // non printable
            {
                if (c != 0x0a && c != 0x0d)   // non LF/CR
                {
                    return true;
                }
            }
        }

        return false;
    }

    class Field
    {
        String name;

        String value;

        public Field(String name, String value)
        {
            this.name = name;
            this.value = value;
        }
    }
}
