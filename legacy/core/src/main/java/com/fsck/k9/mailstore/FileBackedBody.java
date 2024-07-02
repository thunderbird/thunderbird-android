package com.fsck.k9.mailstore;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.RawDataBody;
import com.fsck.k9.mail.internet.SizeAware;
import org.apache.commons.io.IOUtils;


public class FileBackedBody implements Body, SizeAware, RawDataBody {
    private final File file;
    private final String encoding;

    public FileBackedBody(File file, String encoding) {
        this.file = file;
        this.encoding = encoding;
    }

    @Override
    public InputStream getInputStream() throws MessagingException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new MessagingException("File not found", e);
        }
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        throw new RuntimeException("not supported");
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

    @Override
    public String getEncoding() {
        return encoding;
    }
}
