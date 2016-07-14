package com.fsck.k9.mailstore;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.RawDataBody;
import com.fsck.k9.mail.internet.SizeAware;
import com.fsck.k9.service.FileProviderDeferredFileOutputStream;
import com.fsck.k9.service.FileProviderInterface;
import org.apache.commons.io.IOUtils;


/** This is a body where the body can be accessed through a FileProvider.
 * @see FileProviderInterface
 */
public class ProvidedTempFileBody implements RawDataBody, SizeAware {
    public static final int MEMORY_BACKED_THRESHOLD = 1024 * 8;


    private final FileProviderInterface fileProviderInterface;
    private final String encoding;

    @Nullable
    private byte[] data;
    private File file;


    public ProvidedTempFileBody(FileProviderInterface fileProviderInterface, String transferEncoding) {
        this.fileProviderInterface = fileProviderInterface;
        this.encoding = transferEncoding;
    }

    public OutputStream getOutputStream() throws IOException {
        return new FileProviderDeferredFileOutputStream(MEMORY_BACKED_THRESHOLD, fileProviderInterface) {
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

    public Uri getProviderUri(String mimeType) throws IOException {
        if (file == null) {
            writeMemoryToFile();
        }
        return fileProviderInterface.getUriForProvidedFile(file, encoding, mimeType);
    }

    private void writeMemoryToFile() throws IOException {
        if (file != null) {
            throw new IllegalStateException("Body is already file-backed!");
        }
        if (data == null) {
            throw new IllegalStateException("Data must be fully written before it can be read!");
        }

        Log.d(K9.LOG_TAG, "Writing body to file for attachment access");

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
