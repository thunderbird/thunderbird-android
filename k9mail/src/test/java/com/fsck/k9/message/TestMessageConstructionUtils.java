package com.fsck.k9.message;


import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;


public class TestMessageConstructionUtils {
    public static MimeMessage messageFromBody(BodyPart bodyPart) throws MessagingException {
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, bodyPart.getBody());
        if (bodyPart.getContentType() != null) {
            message.setHeader("Content-Type", bodyPart.getContentType());
        }
        message.setUid("msguid");
        return message;
    }

    public static MimeBodyPart multipart(String type, BodyPart... subParts) throws MessagingException {
        return multipart(type, null, subParts);
    }

    public static MimeBodyPart multipart(String type, String typeParameters, BodyPart... subParts) throws MessagingException {
        MimeMultipart multiPart = MimeMultipart.newInstance();
        multiPart.setSubType(type);
        for (BodyPart subPart : subParts) {
            multiPart.addBodyPart(subPart);
        }
        MimeBodyPart mimeBodyPart = new MimeBodyPart(multiPart);
        if (typeParameters != null) {
            mimeBodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                    mimeBodyPart.getContentType() + "; " + typeParameters);
        }
        return mimeBodyPart;
    }

    public static BodyPart bodypart(String type) throws MessagingException {
        return new MimeBodyPart(null, type);
    }

    public static MimeBodyPart bodypart(String type, String text) throws MessagingException {
        TextBody textBody = new TextBody(text);
        return new MimeBodyPart(textBody, type);
    }

    public static BodyPart bodypart(String type, Body body) throws MessagingException {
        return new MimeBodyPart(body, type);
    }
}
