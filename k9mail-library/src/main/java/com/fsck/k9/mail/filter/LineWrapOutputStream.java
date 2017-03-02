package com.fsck.k9.mail.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LineWrapOutputStream extends FilterOutputStream {
    private static final byte[] CRLF = new byte[] {'\r', '\n'};
    private static final byte[] canIgnore = new byte[] {' ', '\r', '\n'};
    private static final int extraStore = canIgnore.length;

    private byte[] buffer;
    private int bufferStart = 0;
    private int lineLength = 0;
    private int maxLength;


    private int findSplitLocation(){
        if (buffer[maxLength] == ' ' || buffer[maxLength] == '\r')
            return maxLength;
        // Find last space
        int location = maxLength;
        for (int i = maxLength - 1; i >= 0 ; i--) {
            if (buffer[i] == ' '){
                location = i;
                break;
            }
        }
        return location;
    }

    public LineWrapOutputStream(OutputStream out, int maxLineLength) {
        super(out);
        maxLength = maxLineLength -2;
        buffer = new byte[maxLength + extraStore];

    }

    @Override
    public void write(int oneByte) throws IOException {
        // Buffer full?
        if (lineLength == buffer.length) {
            int splitLocation = findSplitLocation();
            out.write(buffer, bufferStart, splitLocation);
            out.write(CRLF);
            int newStart = splitLocation;
            for (int i = splitLocation; i < buffer.length; i++) {
                if (buffer[i] == ' ' || buffer[i] == '\r' || buffer[i] == '\n')
                    newStart++;
                else
                    break;
            }
            int newLine = buffer.length - newStart;
            System.arraycopy(buffer,newStart,buffer,0,newLine);
            bufferStart = 0;
            lineLength = newLine;
        }

        if (((oneByte == '\n') || (oneByte == '\r')) && lineLength <= maxLength) {
            // <CR> or <LF> character found, so output buffer ...
            if (lineLength - bufferStart > 0) {
                out.write(buffer, bufferStart, lineLength - bufferStart);
            }
            // ... and that character
            out.write(oneByte);
            lineLength = 0;
            bufferStart = 0;
        } else {
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
        }
        out.flush();
    }
}
