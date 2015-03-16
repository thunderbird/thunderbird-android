
package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;

import java.io.*;
import java.util.Locale;
import java.util.Random;

public class MimeMultipart extends Multipart {
    private String mimeType;
    private byte[] preamble;
    private byte[] epilogue;
    private final String boundary;

    public MimeMultipart() throws MessagingException {
        boundary = generateBoundary();
        setSubType("mixed");
    }

    public MimeMultipart(String mimeType, String boundary) throws MessagingException {
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType can't be null");
        }
        if (boundary == null) {
            throw new IllegalArgumentException("boundary can't be null");
        }

        this.mimeType = mimeType;
        this.boundary = boundary;
    }

    public String generateBoundary() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append("----");
        for (int i = 0; i < 30; i++) {
            sb.append(Integer.toString(random.nextInt(36), 36));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    @Override
    public String getBoundary() {
        return boundary;
    }

    public byte[] getPreamble() {
        return preamble;
    }

    public void setPreamble(byte[] preamble) {
        this.preamble = preamble;
    }

    public byte[] getEpilogue() {
        return epilogue;
    }

    public void setEpilogue(byte[] epilogue) {
        this.epilogue = epilogue;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public void setSubType(String subType) {
        mimeType = "multipart/" + subType;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);

        if (preamble != null) {
            out.write(preamble);
            writer.write("\r\n");
        }

        if (getBodyParts().isEmpty()) {
            writer.write("--");
            writer.write(boundary);
            writer.write("\r\n");
        } else {
            for (BodyPart bodyPart : getBodyParts()) {
                writer.write("--");
                writer.write(boundary);
                writer.write("\r\n");
                writer.flush();
                bodyPart.writeTo(out);
                writer.write("\r\n");
            }
        }

        writer.write("--");
        writer.write(boundary);
        writer.write("--\r\n");
        writer.flush();
        if (epilogue != null) {
            out.write(epilogue);
        }
    }

    @Override
    public InputStream getInputStream() throws MessagingException {
        return null;
    }

    @Override
    public void setUsing7bitTransport() throws MessagingException {
        for (BodyPart part : getBodyParts()) {
            part.setUsing7bitTransport();
        }
    }
}
