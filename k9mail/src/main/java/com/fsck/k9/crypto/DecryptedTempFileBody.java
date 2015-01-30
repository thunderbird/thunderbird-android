package com.fsck.k9.crypto;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.RawDataBody;
import com.fsck.k9.mail.internet.SizeAware;
import org.apache.commons.io.IOUtils;


public class DecryptedTempFileBody implements RawDataBody, SizeAware {
    private final File tempDirectory;
    private final String encoding;
    private File file;


    public DecryptedTempFileBody(String encoding, File tempDirectory) {
        this.encoding = encoding;
        this.tempDirectory = tempDirectory;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        throw new RuntimeException("Not supported");
    }

    public OutputStream getOutputStream() throws IOException {
        file = File.createTempFile("decrypted", null, tempDirectory);
        return new FileOutputStream(file);
    }

    @Override
    public InputStream getInputStream() throws MessagingException {
        try {
            return new FileInputStream(file);
        } catch (IOException ioe) {
            throw new MessagingException("Unable to open body", ioe);
        }
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        InputStream in = getInputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            in.close();
        }
    }

    @Override
    public long getSize() {
        return file.length();
    }

    public File getFile() {
        return file;
    }
}
