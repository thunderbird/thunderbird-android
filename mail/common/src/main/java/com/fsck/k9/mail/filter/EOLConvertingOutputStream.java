package com.fsck.k9.mail.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class EOLConvertingOutputStream extends FilterOutputStream {
    private static final int CR = '\r';
    private static final int LF = '\n';


    private int lastByte;
    private boolean ignoreLf = false;


    public EOLConvertingOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (oneByte == LF && ignoreLf) {
            ignoreLf = false;
            return;
        }
        if (oneByte == LF && lastByte != CR) {
            writeByte(CR);
        } else if (oneByte != LF && lastByte == CR) {
            writeByte(LF);
        }
        writeByte(oneByte);
        ignoreLf = false;
    }

    @Override
    public void flush() throws IOException {
        completeCrLf();
        super.flush();
    }

    public void endWithCrLfAndFlush() throws IOException {
        completeCrLf();
        if (lastByte != LF) {
            writeByte(CR);
            writeByte(LF);
        }
        super.flush();
    }

    private void completeCrLf() throws IOException {
        if (lastByte == CR) {
            writeByte(LF);
            // We have to ignore the next character if it is <LF>. Otherwise it
            // will be expanded to an additional <CR><LF> sequence although it
            // belongs to the one just completed.
            ignoreLf = true;
        }
    }

    private void writeByte(int oneByte) throws IOException {
        super.write(oneByte);
        lastByte = oneByte;
    }
}
