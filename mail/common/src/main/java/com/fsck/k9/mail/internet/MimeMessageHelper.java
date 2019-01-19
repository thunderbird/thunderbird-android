package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import org.apache.james.mime4j.util.MimeUtil;


public class MimeMessageHelper {
    private MimeMessageHelper() {
    }

    public static void setBody(Part part, Body body) throws MessagingException {
        part.setBody(body);

        if (part instanceof Message) {
            part.setHeader("MIME-Version", "1.0");
        }

        if (body instanceof Multipart) {
            Multipart multipart = ((Multipart) body);
            multipart.setParent(part);
            String contentType = Headers.contentTypeForMultipart(multipart.getMimeType(), multipart.getBoundary());
            part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);
            // note: if this is ever changed to 8bit, multipart/signed parts must always be 7bit!
            setEncoding(part, MimeUtil.ENC_7BIT);
        } else if (body instanceof TextBody) {
            MimeValue contentTypeHeader = MimeParameterDecoder.decode(part.getContentType());
            String mimeType = contentTypeHeader.getValue();
            if (MimeUtility.mimeTypeMatches(mimeType, "text/*")) {
                String name = contentTypeHeader.getParameters().get("name");

                String contentType = Headers.contentType(mimeType, "utf-8", name);
                part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);
            } else {
                part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, mimeType);
            }

            setEncoding(part, MimeUtil.ENC_QUOTED_PRINTABLE);
        } else if (body instanceof RawDataBody) {
            String encoding = ((RawDataBody) body).getEncoding();
            part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);
        }
    }

    public static void setEncoding(Part part, String encoding) throws MessagingException {
        Body body = part.getBody();
        if (body != null) {
            body.setEncoding(encoding);
        }
        part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);
    }
}
