package com.fsck.k9.mailstore;


import java.io.ByteArrayOutputStream;
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
import com.fsck.k9.mailstore.util.FileFactory;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.io.EOLConvertingInputStream;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;

public class MimePartStreamParser {

    public static MimeBodyPart parse(FileFactory fileFactory, InputStream inputStream)
            throws MessagingException, IOException {
        MimeBodyPart parsedRootPart = new MimeBodyPart();

        MimeConfig parserConfig  = new MimeConfig.Builder()
                .setMaxHeaderLen(-1)
                .setMaxLineLen(-1)
                .setMaxHeaderCount(-1)
                .build();

        MimeStreamParser parser = new MimeStreamParser(parserConfig);
        parser.setContentHandler(new PartBuilder(parser, fileFactory, parsedRootPart));
        parser.setRecurse();

        try {
            parser.parse(new EOLConvertingInputStream(inputStream));
        } catch (MimeException e) {
            throw new MessagingException("Failed to parse decrypted content", e);
        }

        return parsedRootPart;
    }

    private static Body createBody(InputStream inputStream, String transferEncoding,
            FileFactory fileFactory) throws IOException {
        DeferredFileBody body = new DeferredFileBody(fileFactory, transferEncoding);
        OutputStream outputStream = body.getOutputStream();
        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            outputStream.close();
        }

        return body;
    }


    private static class PartBuilder implements ContentHandler {
        private MimeStreamParser parser;
        private final FileFactory fileFactory;
        private final MimeBodyPart decryptedRootPart;
        private final Stack<Object> stack = new Stack<>();

        private boolean isMessagePart;
        private boolean isContentDispositionAttachment;

        PartBuilder(MimeStreamParser parser, FileFactory fileFactory,
                MimeBodyPart decryptedRootPart) {
            this.parser = parser;
            this.fileFactory = fileFactory;
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
            parser.setRecurse();
        }

        @Override
        public void startHeader() throws MimeException {
            isMessagePart = false;
            isContentDispositionAttachment = false;
        }

        @Override
        public void field(Field parsedField) throws MimeException {
            String name = parsedField.getName();
            String raw = parsedField.getRaw().toString();

            Part part = (Part) stack.peek();
            part.addRawHeader(name, raw);

            String fieldImmediateValue = MimeUtility.getHeaderParameter(parsedField.getBody(), null);
            if ("Content-Type".equalsIgnoreCase(name) && MimeUtility.isMessage(fieldImmediateValue)) {
                isMessagePart = true;
            }

            if ("Content-Disposition".equalsIgnoreCase(name) && "attachment".equalsIgnoreCase(fieldImmediateValue)) {
                isContentDispositionAttachment = true;
            }
        }

        @Override
        public void endHeader() throws MimeException {
            if (isMessagePart && isContentDispositionAttachment) {
                parser.setFlat();
            }
        }

        @Override
        public void preamble(InputStream is) throws MimeException, IOException {
            expect(MimeMultipart.class);
            ByteArrayOutputStream preamble = new ByteArrayOutputStream();
            IOUtils.copy(is, preamble);
            ((MimeMultipart)stack.peek()).setPreamble(preamble.toByteArray());
        }

        @Override
        public void epilogue(InputStream is) throws MimeException, IOException {
            expect(MimeMultipart.class);
            ByteArrayOutputStream epilogue = new ByteArrayOutputStream();
            IOUtils.copy(is, epilogue);
            ((MimeMultipart) stack.peek()).setEpilogue(epilogue.toByteArray());
        }

        @Override
        public void startMultipart(BodyDescriptor bd) throws MimeException {
            Part part = (Part) stack.peek();

            String mimeType = bd.getMimeType();
            String boundary = bd.getBoundary();

            MimeMultipart multipart = new MimeMultipart(mimeType, boundary);
            part.setBody(multipart);

            stack.push(multipart);
        }

        @Override
        public void endMultipart() throws MimeException {
            stack.pop();
        }

        @Override
        public void body(BodyDescriptor bd, InputStream inputStream) throws MimeException, IOException {
            Part part = (Part) stack.peek();

            String transferEncoding = bd.getTransferEncoding();
            Body body = createBody(inputStream, transferEncoding, fileFactory);

            part.setBody(body);
        }

        @Override
        public void raw(InputStream is) throws MimeException, IOException {
            throw new IllegalStateException("Not implemented");
        }

        private void expect(Class<?> c) {
            if (!c.isInstance(stack.peek())) {
                throw new IllegalStateException("Internal stack error: " + "Expected '"
                        + c.getName() + "' found '" + stack.peek().getClass().getName() + "'");
            }
        }
    }
}
