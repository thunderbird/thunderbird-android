package com.fsck.k9.mailstore;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

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
import com.fsck.k9.service.FileProviderInterface;
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

public class MimePartStreamParser {

    public static MimeBodyPart parse(FileProviderInterface fileProviderInterface, InputStream inputStream)
            throws MessagingException, IOException {
        MimeBodyPart parsedRootPart = new MimeBodyPart();

        MimeConfig parserConfig  = new MimeConfig();
        parserConfig.setMaxHeaderLen(-1);
        parserConfig.setMaxLineLen(-1);
        parserConfig.setMaxHeaderCount(-1);

        MimeStreamParser parser = new MimeStreamParser(parserConfig);
        parser.setContentHandler(new PartBuilder(fileProviderInterface, parsedRootPart));
        parser.setRecurse();

        try {
            parser.parse(new EOLConvertingInputStream(inputStream));
        } catch (MimeException e) {
            throw new MessagingException("Failed to parse decrypted content", e);
        }

        return parsedRootPart;
    }

    private static Body createBody(InputStream inputStream, String transferEncoding,
            FileProviderInterface fileProviderInterface) throws IOException {
        ProvidedTempFileBody body = new ProvidedTempFileBody(fileProviderInterface, transferEncoding);
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


    private static class PartBuilder implements ContentHandler {
        private final FileProviderInterface fileProviderInterface;
        private final MimeBodyPart decryptedRootPart;
        private final Stack<Object> stack = new Stack<>();

        public PartBuilder(FileProviderInterface fileProviderInterface, MimeBodyPart decryptedRootPart)
                throws MessagingException {
            this.fileProviderInterface = fileProviderInterface;
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
            Body body = createBody(inputStream, transferEncoding, fileProviderInterface);

            part.setBody(body);
        }

        @Override
        public void raw(InputStream is) throws MimeException, IOException {
            throw new IllegalStateException("Not implemented");
        }
    }
}
