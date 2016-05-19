package com.fsck.k9.mailstore;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.SizeAware;


public class DecryptedTempFileBody extends BinaryAttachmentBody implements SizeAware {
    private final File tempDirectory;
    private File file;


    public DecryptedTempFileBody(File tempDirectory, String transferEncoding) {
        this.tempDirectory = tempDirectory;
        try {
            setEncoding(transferEncoding);
        } catch (MessagingException e) {
            throw new AssertionError("setEncoding() must succeed");
        }
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
    public long getSize() {
        return file.length();
    }

    public File getFile() {
        return file;
    }
}
