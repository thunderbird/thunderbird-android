package com.fsck.k9.mailstore;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.RawDataBody;
import com.fsck.k9.mail.internet.SizeAware;
import com.fsck.k9.mailstore.util.DeferredFileOutputStream;
import com.fsck.k9.mailstore.util.FileFactory;
import org.apache.commons.io.IOUtils;


/** This is a body where the data is memory-backed at first and switches to file-backed if it gets larger.
 * @see FileFactory
 */
public class DeferredFileBody implements RawDataBody, SizeAware {
    public static final int DEFAULT_MEMORY_BACKED_THRESHOLD = 1024 * 8;


    private final FileFactory fileFactory;
    private final String encoding;
    private final int memoryBackedThreshold;

    @Nullable
    private byte[] data;
    private File file;


    public DeferredFileBody(FileFactory fileFactory, String transferEncoding) {
        this(DEFAULT_MEMORY_BACKED_THRESHOLD, fileFactory, transferEncoding);
    }

    @VisibleForTesting
    DeferredFileBody(int memoryBackedThreshold, FileFactory fileFactory,
            String transferEncoding) {
        this.fileFactory = fileFactory;
        this.memoryBackedThreshold = memoryBackedThreshold;
        this.encoding = transferEncoding;
    }

    public OutputStream getOutputStream() throws IOException {
        return new DeferredFileOutputStream(memoryBackedThreshold, fileFactory) {
            @Override
            public void close() throws IOException {
                super.close();

                if (isThresholdExceeded()) {
                    file = getFile();
                } else {
                    data = getData();
                }
            }
        };
    }

    @Override
    public InputStream getInputStream() throws MessagingException {
        try {
            if (file != null) {
                Log.d(K9.LOG_TAG, "Decrypted data is file-backed.");
                return new BufferedInputStream(new FileInputStream(file));
            }
            if (data != null) {
                Log.d(K9.LOG_TAG, "Decrypted data is memory-backed.");
                return new ByteArrayInputStream(data);
            }

            throw new IllegalStateException("Data must be fully written before it can be read!");
        } catch (IOException ioe) {
            throw new MessagingException("Unable to open body", ioe);
        }
    }

    @Override
    public long getSize() {
        if (file != null) {
            return file.length();
        }
        if (data != null) {
            return data.length;
        }

        throw new IllegalStateException("Data must be fully written before it can be read!");
    }

    public File getFile() throws IOException {
        if (file == null) {
            writeMemoryToFile();
        }
        return file;
    }

    private void writeMemoryToFile() throws IOException {
        if (file != null) {
            throw new IllegalStateException("Body is already file-backed!");
        }
        if (data == null) {
            throw new IllegalStateException("Data must be fully written before it can be read!");
        }

        Log.d(K9.LOG_TAG, "Writing body to file for attachment access");

        file = fileFactory.createFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();

        data = null;
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        throw new UnsupportedOperationException("Cannot re-encode a DecryptedTempFileBody!");
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        InputStream inputStream = getInputStream();
        IOUtils.copy(inputStream, out);
    }

    @Override
    public String getEncoding() {
        return encoding;
    }
}
