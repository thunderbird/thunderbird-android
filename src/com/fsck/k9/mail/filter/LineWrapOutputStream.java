package com.fsck.k9.mail.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LineWrapOutputStream extends FilterOutputStream {
    private static final byte[] CRLF = new byte[] {'\r', '\n'};

    private byte[] buffer;
    private int bufferStart = 0;
    private int lineLength = 0;
    private int endOfLastWord = 0;


    public LineWrapOutputStream(OutputStream out, int maxLineLength) {
        super(out);
        buffer = new byte[maxLineLength - 2];
    }

    @Override
    public void write(int oneByte) throws IOException {
        // Buffer full?
        if (lineLength == buffer.length) {
            // Usable word-boundary found earlier?
            if (endOfLastWord > 0) {
                // Yes, so output everything up to that word-boundary
                out.write(buffer, bufferStart, endOfLastWord - bufferStart);
                out.write(CRLF);

                bufferStart = 0;

                // Skip the <SPACE> in the buffer
                endOfLastWord++;
                lineLength = buffer.length - endOfLastWord;
                if (lineLength > 0) {
                    // Copy rest of the buffer to the front
                    System.arraycopy(buffer, endOfLastWord + 0, buffer, 0, lineLength);
                }
                endOfLastWord = 0;
            } else {
                // No word-boundary found, so output whole buffer
                out.write(buffer, bufferStart, buffer.length - bufferStart);
                out.write(CRLF);
                lineLength = 0;
                bufferStart = 0;
            }
        }

        if ((oneByte == '\n') || (oneByte == '\r')) {
            // <CR> or <LF> character found, so output buffer ...
            if (lineLength - bufferStart > 0) {
                out.write(buffer, bufferStart, lineLength - bufferStart);
            }
            // ... and that character
            out.write(oneByte);
            lineLength = 0;
            bufferStart = 0;
            endOfLastWord = 0;
        } else {
            // Remember this position as last word-boundary if <SPACE> found
            if (oneByte == ' ') {
                endOfLastWord = lineLength;
            }

            // Write character to the buffer
            buffer[lineLength] = (byte)oneByte;
            lineLength++;
        }
    }

    @Override
    public void flush() throws IOException {
        // Buffer empty?
        if (lineLength > bufferStart) {
            // Output everything we have up till now
            out.write(buffer, bufferStart, lineLength - bufferStart);

            // Mark current position as new start of the buffer
            bufferStart = (lineLength == buffer.length) ? 0 : lineLength;
            endOfLastWord = 0;
        }
        out.flush();
    }
}
