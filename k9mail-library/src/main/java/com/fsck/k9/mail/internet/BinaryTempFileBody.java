package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;

import java.io.*;

/**
 * A Body that is backed by a temp file. The Body exposes a getOutputStream method that allows
 * the user to write to the temp file. After the write the body is available via getInputStream
 * and writeTo one time. After writeTo is called, or the InputStream returned from
 * getInputStream is closed the file is deleted and the Body should be considered disposed of.
 */
public class BinaryTempFileBody implements RawDataBody, SizeAware {
    private static File mTempDirectory;

    private File mFile;

    String mEncoding = null;

    public static void setTempDirectory(File tempDirectory) {
        mTempDirectory = tempDirectory;
    }

    public static File getTempDirectory() {
        return mTempDirectory;
    }

    @Override
    public String getEncoding() {
        return mEncoding;
    }

    public void setEncoding(String encoding) throws MessagingException {
        if (mEncoding != null && mEncoding.equalsIgnoreCase(encoding)) {
            return;
        }

        // The encoding changed, so we need to convert the message
        if (!MimeUtil.ENC_8BIT.equalsIgnoreCase(mEncoding)) {
            throw new RuntimeException("Can't convert from encoding: " + mEncoding);
        }

        try {
            File newFile = File.createTempFile("body", null, mTempDirectory);
            final OutputStream out = new FileOutputStream(newFile);
            try {
                OutputStream wrappedOut = null;
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

            mFile = newFile;
            mEncoding = encoding;
        } catch (IOException e) {
            throw new MessagingException("Unable to convert body", e);
        }
    }

    public BinaryTempFileBody(String encoding) {
        if (mTempDirectory == null) {
            throw new RuntimeException("setTempDirectory has not been called on BinaryTempFileBody!");
        }

        mEncoding = encoding;
    }

    public OutputStream getOutputStream() throws IOException {
        mFile = File.createTempFile("body", null, mTempDirectory);
        mFile.deleteOnExit();
        return new FileOutputStream(mFile);
    }

    public InputStream getInputStream() throws MessagingException {
        try {
            return new BinaryTempFileBodyInputStream(new FileInputStream(mFile));
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
        return mFile.length();
    }

    public File getFile() {
        return mFile;
    }

    class BinaryTempFileBodyInputStream extends FilterInputStream {
        public BinaryTempFileBodyInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                mFile.delete();
            }
        }

        public void closeWithoutDeleting() throws IOException {
            super.close();
        }
    }
}
