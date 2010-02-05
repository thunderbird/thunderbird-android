package com.fsck.k9.mail.transport;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class EOLConvertingOutputStream extends FilterOutputStream
{
    private int lastChar;
    private boolean ignoreNextIfLF = false;

    public EOLConvertingOutputStream(OutputStream out)
    {
        super(out);
    }

    @Override
    public void write(int oneByte) throws IOException
    {
        if (!ignoreNextIfLF)
        {
            if ((oneByte == '\n') && (lastChar != '\r'))
            {
                super.write('\r');
            }
            super.write(oneByte);
            lastChar = oneByte;
        }
        ignoreNextIfLF = false;
    }

    @Override
    public void flush() throws IOException
    {
        if (lastChar == '\r')
        {
            super.write('\n');
            lastChar = '\n';
            
            // We have to ignore the next character if it is <LF>. Otherwise it
            // will be expanded to an additional <CR><LF> sequence although it
            // belongs to the one just completed.
            ignoreNextIfLF = true;
        }
        super.flush();
    }
}
