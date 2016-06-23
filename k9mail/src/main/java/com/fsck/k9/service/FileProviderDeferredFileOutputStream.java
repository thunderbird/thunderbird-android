package com.fsck.k9.service;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.commons.io.output.ThresholdingOutputStream;


/**
 * This OutputStream is modelled after apache commons' {@link DeferredFileOutputStream},
 * but uses a {@link FileProviderInterface} instead of directly generating temporary files
 * itself.
 */
public class FileProviderDeferredFileOutputStream extends ThresholdingOutputStream {


    private ByteArrayOutputStream memoryOutputStream;
    private OutputStream currentOutputStream;
    private File outputFile;
    private final FileProviderInterface fileProviderInterface;


    public FileProviderDeferredFileOutputStream(int threshold, FileProviderInterface fileProviderInterface) {
        super(threshold);
        memoryOutputStream = new ByteArrayOutputStream();
        currentOutputStream = memoryOutputStream;
        this.fileProviderInterface = fileProviderInterface;
    }


    @Override
    protected OutputStream getStream() throws IOException {
        return currentOutputStream;
    }


    @Override
    protected void thresholdReached() throws IOException {
        outputFile = fileProviderInterface.createProvidedFile();
        FileOutputStream fos = new FileOutputStream(outputFile);
        memoryOutputStream.writeTo(fos);
        currentOutputStream = fos;
        memoryOutputStream = null;
    }

    public byte[] getData() {
        if (memoryOutputStream != null)
        {
            return memoryOutputStream.toByteArray();
        }
        return null;
    }

    public File getFile() {
        return outputFile;
    }

}
