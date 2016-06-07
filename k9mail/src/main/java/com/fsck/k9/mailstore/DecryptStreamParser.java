package com.fsck.k9.mailstore;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.io.EOLConvertingInputStream;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.mime4j.util.MimeUtil;

// TODO rename this class? this class doesn't really bear any 'decrypted' semantics anymore...
public class DecryptStreamParser {

    private static final String DECRYPTED_CACHE_DIRECTORY = "decrypted";

    public static MimeBodyPart parse(Context context, InputStream inputStream) throws MessagingException, IOException {
        inputStream = new BufferedInputStream(inputStream, 4096);

        boolean hasInputData = waitForInputData(inputStream);
        if (!hasInputData) {
            return null;
        }

        File decryptedTempDirectory = getDecryptedTempDirectory(context);

        MimeBodyPart decryptedRootPart = new MimeBodyPart();

        MimeConfig parserConfig  = new MimeConfig();
        parserConfig.setMaxHeaderLen(-1);
        parserConfig.setMaxLineLen(-1);
        parserConfig.setMaxHeaderCount(-1);

        MimeStreamParser parser = new MimeStreamParser(parserConfig);
        parser.setContentHandler(new PartBuilder(decryptedTempDirectory, decryptedRootPart));
        parser.setRecurse();

        try {
            parser.parse(new EOLConvertingInputStream(inputStream));
        } catch (MimeException e) {
            throw new MessagingException("Failed to parse decrypted content", e);
        }

        return decryptedRootPart;
    }

    private static boolean waitForInputData(InputStream inputStream) {
        try {
            inputStream.mark(1);
            int ret = inputStream.read();
            inputStream.reset();
            return ret != -1;
        } catch (IOException e) {
            Log.d(K9.LOG_TAG, "got no input from pipe", e);
            return false;
        }
    }

    private static Body createBody(InputStream inputStream, String transferEncoding, File decryptedTempDirectory)
            throws IOException {
        DecryptedTempFileBody body = new DecryptedTempFileBody(decryptedTempDirectory, transferEncoding);
        OutputStream outputStream = body.getOutputStream();
        try {
            InputStream decodingInputStream;
            boolean closeStream;
            if (MimeUtil.ENC_QUOTED_PRINTABLE.equals(transferEncoding)) {
                decodingInputStream = new QuotedPrintableInputStream(inputStream, false);
                closeStream = true;
            } else if (MimeUtil.ENC_BASE64.equals(transferEncoding)) {
                decodingInputStream = new Base64InputStream(inputStream);
                closeStream = true;
            } else {
                decodingInputStream = inputStream;
                closeStream = false;
            }

            try {
                IOUtils.copy(decodingInputStream, outputStream);
            } finally {
                if (closeStream) {
                    decodingInputStream.close();
                }
            }
        } finally {
            outputStream.close();
        }

        return body;
    }

    private static File getDecryptedTempDirectory(Context context) {
        File directory = new File(context.getCacheDir(), DECRYPTED_CACHE_DIRECTORY);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                Log.e(K9.LOG_TAG, "Error creating directory: " + directory.getAbsolutePath());
            }
        }

        return directory;
    }


    private static class PartBuilder implements ContentHandler {
        private final File decryptedTempDirectory;
        private final MimeBodyPart decryptedRootPart;
        private final Stack<Object> stack = new Stack<Object>();

        public PartBuilder(File decryptedTempDirectory, MimeBodyPart decryptedRootPart)
                throws MessagingException {
            this.decryptedTempDirectory = decryptedTempDirectory;
            this.decryptedRootPart = decryptedRootPart;
        }

        @Override
        public void startMessage() throws MimeException {
            if (stack.isEmpty()) {
                stack.push(decryptedRootPart);
            } else {
                Part part = (Part) stack.peek();

                Message innerMessage = new MimeMessage();
                part.setBody(innerMessage);

                stack.push(innerMessage);
            }
        }

        @Override
        public void endMessage() throws MimeException {
            stack.pop();
        }

        @Override
        public void startBodyPart() throws MimeException {
            try {
                Multipart multipart = (Multipart) stack.peek();

                BodyPart bodyPart = new MimeBodyPart();
                multipart.addBodyPart(bodyPart);

                stack.push(bodyPart);
            } catch (MessagingException e) {
                throw new MimeException(e);
            }
        }

        @Override
        public void endBodyPart() throws MimeException {
            stack.pop();
        }

        @Override
        public void startHeader() throws MimeException {
            // Do nothing
        }

        @Override
        public void field(Field parsedField) throws MimeException {
            try {
                String name = parsedField.getName();
                String raw = parsedField.getRaw().toString();

                Part part = (Part) stack.peek();
                part.addRawHeader(name, raw);
            } catch (MessagingException e) {
                throw new MimeException(e);
            }
        }

        @Override
        public void endHeader() throws MimeException {
            // Do nothing
        }

        @Override
        public void preamble(InputStream is) throws MimeException, IOException {
            // Do nothing
        }

        @Override
        public void epilogue(InputStream is) throws MimeException, IOException {
            // Do nothing
        }

        @Override
        public void startMultipart(BodyDescriptor bd) throws MimeException {
            Part part = (Part) stack.peek();
            try {
                String contentType = part.getContentType();
                String mimeType = MimeUtility.getHeaderParameter(contentType, null);
                String boundary = MimeUtility.getHeaderParameter(contentType, "boundary");

                MimeMultipart multipart = new MimeMultipart(mimeType, boundary);
                part.setBody(multipart);

                stack.push(multipart);
            } catch (MessagingException e) {
                throw new MimeException(e);
            }
        }

        @Override
        public void endMultipart() throws MimeException {
            stack.pop();
        }

        @Override
        public void body(BodyDescriptor bd, InputStream inputStream) throws MimeException, IOException {
            Part part = (Part) stack.peek();

            String transferEncoding = bd.getTransferEncoding();
            Body body = createBody(inputStream, transferEncoding, decryptedTempDirectory);

            part.setBody(body);
        }

        @Override
        public void raw(InputStream is) throws MimeException, IOException {
            throw new IllegalStateException("Not implemented");
        }
    }
}
