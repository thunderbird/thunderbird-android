package com.fsck.k9.mail.internet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;

class Iso2022JpToShiftJisInputStream extends InputStream {
    private enum Charset {
        ASCII, JISX0201, JISX0208,
    }

    private InputStream mIn;
    private Charset charset = Charset.ASCII;
    private int out;
    private boolean hasOut = false;

    public Iso2022JpToShiftJisInputStream(InputStream in) {
        mIn = in;
    }

    @Override
    public int read() throws IOException {
        if (hasOut) {
            hasOut = false;
            return out;
        }

        int in1 = mIn.read();
        while (in1 == 0x1b) {
            in1 = mIn.read();
            if (in1 == '(') {
                in1 = mIn.read();
                if (in1 == 'B' || in1 == 'J')
                    charset = Charset.ASCII;
                else if (in1 == 'I')  // Not defined in RFC 1468 but in CP50221.
                    charset = Charset.JISX0201;
                else
                    throw new MalformedInputException(0);
            } else if (in1 == '$') {
                in1 = mIn.read();
                if (in1 == '@' || in1 == 'B')
                    charset = Charset.JISX0208;
                else
                    throw new MalformedInputException(0);
            } else
                throw new MalformedInputException(0);
            in1 = mIn.read();
        }

        if (in1 == '\n' || in1 == '\r')
            charset = Charset.ASCII;

        if (in1 < 0x21 || in1 >= 0x7f)
            return in1;

        switch (charset) {
        case ASCII:
            return in1;
        case JISX0201:
            return in1 + 0x80;
        case JISX0208:
            int in2 = mIn.read();
            if (in2 < 0x21 || in2 >= 0x7f)
                throw new MalformedInputException(0);

            int out1 = (in1 + 1) / 2 + (in1 < 0x5f ? 0x70 : 0xb0);
            int out2 = in2 + (in1 % 2 == 0 ? 0x7e : in2 < 0x60 ? 0x1f : 0x20);

            out = out2;
            hasOut = true;

            return out1;
        default:
            throw new RuntimeException();
        }
    }
}
