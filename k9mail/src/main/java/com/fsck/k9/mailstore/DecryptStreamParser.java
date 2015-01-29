package com.fsck.k9.mailstore;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.io.EOLConvertingInputStream;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;


public class DecryptStreamParser {
    public static DecryptedBodyPart parse(InputStream inputStream) throws MessagingException, IOException {
        DecryptedBodyPart decryptedRootPart = new DecryptedBodyPart();

        MimeConfig parserConfig  = new MimeConfig();
        parserConfig.setMaxHeaderLen(-1);
        parserConfig.setMaxLineLen(-1);
        parserConfig.setMaxHeaderCount(-1);

        MimeStreamParser parser = new MimeStreamParser(parserConfig);
        parser.setContentHandler(new PartBuilder(decryptedRootPart));
        parser.setRecurse();

        try {
            parser.parse(new EOLConvertingInputStream(inputStream));
        } catch (MimeException e) {
            throw new MessagingException("Failed to parse decrypted content", e);
        }

        return decryptedRootPart;
    }

    private static Body createBody(InputStream inputStream, String transferEncoding) throws IOException {
        //TODO: only read parts we're going to display into memory
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, byteArrayOutputStream);
        } finally {
            byteArrayOutputStream.close();
        }

        byte[] data = byteArrayOutputStream.toByteArray();

        return new BinaryMemoryBody(data, transferEncoding);
    }


    private static class PartBuilder  implements ContentHandler {
        private final DecryptedBodyPart decryptedRootPart;
        private final Stack<Object> stack = new Stack<Object>();

        public PartBuilder(DecryptedBodyPart decryptedRootPart) throws MessagingException {
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
            Body body = createBody(inputStream, transferEncoding);

            part.setBody(body);
        }

        @Override
        public void raw(InputStream is) throws MimeException, IOException {
            throw new IllegalStateException("Not implemented");
        }
    }

    public static class DecryptedBodyPart extends MimeBodyPart {
        public DecryptedBodyPart() throws MessagingException {
            // Do nothing
        }
    }
}
