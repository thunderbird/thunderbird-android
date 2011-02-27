package com.fsck.k9.mail.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SmtpDataStuffing extends FilterOutputStream {
    private static final int STATE_NORMAL = 0;
    private static final int STATE_CR = 1;
    private static final int STATE_CRLF = 2;

    private int state = STATE_NORMAL;

    public SmtpDataStuffing(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (oneByte == '\r') {
            state = STATE_CR;
        } else if ((state == STATE_CR) && (oneByte == '\n')) {
            state = STATE_CRLF;
        } else if ((state == STATE_CRLF) && (oneByte == '.')) {
            // Read <CR><LF><DOT> so this line needs an additional period.
            super.write('.');
            state = STATE_NORMAL;
        } else {
            state = STATE_NORMAL;
        }
        super.write(oneByte);
    }
}
