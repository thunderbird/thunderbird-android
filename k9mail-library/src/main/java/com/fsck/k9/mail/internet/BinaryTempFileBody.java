package com.fsck.k9.mail.internet;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;
import timber.log.Timber;


/**
 * A Body that is backed by a temp file. The Body exposes a getOutputStream method that allows
 * the user to write to the temp file. After the write the body is available via getInputStream
 * and writeTo one time. After writeTo is called, or the InputStream returned from
 * getInputStream is closed the file is deleted and the Body should be considered disposed of.
 */
public class BinaryTempFileBody implements RawDataBody, SizeAware {
    private static File tempDirectory;

    private File file;

    String encoding = null;

    public static void setTempDirectory(File tempDirectory) {
        BinaryTempFileBody.tempDirectory = tempDirectory;
    }

    public static File getTempDirectory() {
        return tempDirectory;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) throws MessagingException {
        if (this.encoding != null && this.encoding.equalsIgnoreCase(encoding)) {
            return;
        }

        // The encoding changed, so we need to convert the message
        if (!MimeUtil.ENC_8BIT.equalsIgnoreCase(this.encoding)) {
            throw new RuntimeException("Can't convert from encoding: " + this.encoding);
        }

        try {
            File newFile = File.createTempFile("body", null, tempDirectory);
            final OutputStream out = new FileOutputStream(newFile);
            try {
                OutputStream wrappedOut;
                if (MimeUtil.ENC_QUOTED_PRINTABLE.equals(encoding)) {
                    wrappedOut = new QuotedPrintableOutputStream(out, false);
                } else if (MimeUtil.ENC_BASE64.equals(encoding)) {
                    wrappedOut = new Base64OutputStream(out);
                } else {
                    throw new RuntimeException("Target encoding not supported: " + encoding);
                }

                InputStream in = getInputStream();
                try {
                    IOUtils.copy(in, wrappedOut);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(wrappedOut);
                }
            } finally {
                IOUtils.closeQuietly(out);
            }

            file = newFile;
            this.encoding = encoding;
        } catch (IOException e) {
            throw new MessagingException("Unable to convert body", e);
        }
    }

    public BinaryTempFileBody(String encoding) {
        if (tempDirectory == null) {
            throw new RuntimeException("setTempDirectory has not been called on BinaryTempFileBody!");
        }

        this.encoding = encoding;
    }

    public OutputStream getOutputStream() throws IOException {
        file = File.createTempFile("body", null, tempDirectory);
        file.deleteOnExit();
        return new FileOutputStream(file);
    }

    @NonNull
    public InputStream getInputStream() throws MessagingException {
        try {
            return new BinaryTempFileBodyInputStream(new FileInputStream(file));
        } catch (IOException ioe) {
            throw new MessagingException("Unable to open body", ioe);
        }
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException {
        InputStream in = getInputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public long getSize() {
        return file.length();
    }

    public File getFile() {
        return file;
    }

    class BinaryTempFileBodyInputStream extends FilterInputStream {
        BinaryTempFileBodyInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                Timber.d("Deleting temporary binary file: %s", file.getName());
                boolean fileSuccessfullyDeleted = file.delete();
                if (!fileSuccessfullyDeleted) {
                    Timber.i("Failed to delete temporary binary file: %s", file.getName());
                }
            }
        }

        void closeWithoutDeleting() throws IOException {
            super.close();
        }
    }
}