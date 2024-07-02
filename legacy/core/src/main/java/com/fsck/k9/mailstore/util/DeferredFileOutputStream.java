package com.fsck.k9.mailstore.util;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.ThresholdingOutputStream;


/**
 * This OutputStream is modelled after apache commons' {@link org.apache.commons.io.output.DeferredFileOutputStream},
 * but uses a {@link FileFactory} instead of directly generating temporary files itself.
 */
public class DeferredFileOutputStream extends ThresholdingOutputStream {
    private final FileFactory fileFactory;

    private OutputStream currentOutputStream;
    private File outputFile;


    public DeferredFileOutputStream(int threshold, FileFactory fileFactory) {
        super(threshold);
        this.fileFactory = fileFactory;

        // scale it so we expand the ByteArrayOutputStream at most three times (assuming quadratic growth)
        int size = threshold < 1024 ? 256 : threshold / 4;
        currentOutputStream = new ByteArrayOutputStream(size);
    }

    @Override
    protected OutputStream getStream() throws IOException {
        return currentOutputStream;
    }

    private boolean isMemoryBacked() {
        return currentOutputStream instanceof ByteArrayOutputStream;
    }

    @Override
    protected void thresholdReached() throws IOException {
        if (outputFile != null) {
            throw new IllegalStateException("thresholdReached must not be called if we already have an output file!");
        }
        if (!isMemoryBacked()) {
            throw new IllegalStateException("currentOutputStream must be memory-based at this point!");
        }
        ByteArrayOutputStream memoryOutputStream = (ByteArrayOutputStream) currentOutputStream;

        outputFile = fileFactory.createFile();
        currentOutputStream = new FileOutputStream(outputFile);

        memoryOutputStream.writeTo(currentOutputStream);
    }

    public byte[] getData() {
        if (!isMemoryBacked()) {
            throw new IllegalStateException("getData must only be called in memory-backed state!");
        }
        return ((ByteArrayOutputStream) currentOutputStream).toByteArray();
    }

    public File getFile() {
        if (isMemoryBacked()) {
            throw new IllegalStateException("getFile must only be called in file-backed state!");
        }
        return outputFile;
    }

}
