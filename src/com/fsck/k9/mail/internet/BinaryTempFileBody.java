package com.fsck.k9.mail.internet;

import com.fsck.k9.codec.binary.Base64OutputStream;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * A Body that is backed by a temp file. The Body exposes a getOutputStream method that allows
 * the user to write to the temp file. After the write the body is available via getInputStream
 * and writeTo one time. After writeTo is called, or the InputStream returned from
 * getInputStream is closed the file is deleted and the Body should be considered disposed of.
 */
public class BinaryTempFileBody implements Body
{
    private static File mTempDirectory;

    private File mFile;

    public static void setTempDirectory(File tempDirectory)
    {
        mTempDirectory = tempDirectory;
    }

    public BinaryTempFileBody() throws IOException
    {
        if (mTempDirectory == null)
        {
            throw new
            RuntimeException("setTempDirectory has not been called on BinaryTempFileBody!");
        }
    }

    public OutputStream getOutputStream() throws IOException
    {
        mFile = File.createTempFile("body", null, mTempDirectory);
        mFile.deleteOnExit();
        return new FileOutputStream(mFile);
    }

    public InputStream getInputStream() throws MessagingException
    {
        try
        {
            return new BinaryTempFileBodyInputStream(new FileInputStream(mFile));
        }
        catch (IOException ioe)
        {
            throw new MessagingException("Unable to open body", ioe);
        }
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException
    {
        InputStream in = getInputStream();
        Base64OutputStream base64Out = new Base64OutputStream(out);
        IOUtils.copy(in, base64Out);
        base64Out.close();
        mFile.delete();
    }

    class BinaryTempFileBodyInputStream extends FilterInputStream
    {
        public BinaryTempFileBodyInputStream(InputStream in)
        {
            super(in);
        }

        @Override
        public void close() throws IOException
        {
            super.close();
            mFile.delete();
        }
    }
}
