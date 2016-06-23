package com.fsck.k9.message.extractors;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalPart;
import com.fsck.k9.mailstore.ProvidedTempFileBody;
import com.fsck.k9.provider.AttachmentProvider;


public class AttachmentInfoExtractor {
    private AttachmentInfoExtractor() { }

    public static AttachmentInfoExtractor getInstance() {
        return new AttachmentInfoExtractor();
    }

    public List<AttachmentViewInfo> extractAttachmentInfos(List<Part> attachmentParts)
            throws MessagingException {

        List<AttachmentViewInfo> attachments = new ArrayList<>();
        for (Part part : attachmentParts) {
            attachments.add(extractAttachmentInfo(part));
        }

        return attachments;
    }

    public AttachmentViewInfo extractAttachmentInfo(Part part) throws MessagingException {
        Uri uri;
        long size;
        if (part instanceof LocalPart) {
            LocalPart localPart = (LocalPart) part;
            String accountUuid = localPart.getAccountUuid();
            long messagePartId = localPart.getId();
            size = localPart.getSize();
            uri = AttachmentProvider.getAttachmentUri(accountUuid, messagePartId);
        } else {
            Body body = part.getBody();
            if (body instanceof ProvidedTempFileBody) {
                ProvidedTempFileBody decryptedTempFileBody = (ProvidedTempFileBody) body;
                size = decryptedTempFileBody.getSize();
                try {
                    uri = decryptedTempFileBody.getProviderUri(part.getMimeType());
                } catch (IOException e) {
                    Log.e(K9.LOG_TAG, "Decrypted temp file (no longer?) exists!", e);
                    uri = null;
                }
                return extractAttachmentInfo(part, uri, size);
            } else {
                throw new IllegalArgumentException("Unsupported part type provided");
            }
        }

        return extractAttachmentInfo(part, uri, size);
    }

    public AttachmentViewInfo extractAttachmentInfoForDatabase(Part part) throws MessagingException {
        return extractAttachmentInfo(part, Uri.EMPTY, AttachmentViewInfo.UNKNOWN_SIZE);
    }

    private AttachmentViewInfo extractAttachmentInfo(Part part, Uri uri, long size) throws MessagingException {
        boolean firstClassAttachment = true;

        String mimeType = part.getMimeType();
        String contentTypeHeader = MimeUtility.unfoldAndDecode(part.getContentType());
        String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());

        String name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        if (name == null) {
            name = MimeUtility.getHeaderParameter(contentTypeHeader, "name");
        }

        if (name == null) {
            firstClassAttachment = false;
            String extension = null;
            if (mimeType != null) {
                extension = MimeUtility.getExtensionByMimeType(mimeType);
            }
            name = "noname" + ((extension != null) ? "." + extension : "");
        }

        // Inline parts with a content-id are almost certainly components of an HTML message
        // not attachments. Only show them if the user pressed the button to show more
        // attachments.
        if (contentDisposition != null &&
                MimeUtility.getHeaderParameter(contentDisposition, null).matches("^(?i:inline)") &&
                part.getHeader(MimeHeader.HEADER_CONTENT_ID).length > 0) {
            firstClassAttachment = false;
        }

        long attachmentSize = extractAttachmentSize(contentDisposition, size);

        return new AttachmentViewInfo(mimeType, name, attachmentSize, uri, firstClassAttachment, part);
    }

    private long extractAttachmentSize(String contentDisposition, long size) {
        if (size != AttachmentViewInfo.UNKNOWN_SIZE) {
            return size;
        }

        long result = AttachmentViewInfo.UNKNOWN_SIZE;
        String sizeParam = MimeUtility.getHeaderParameter(contentDisposition, "size");
        if (sizeParam != null) {
            try {
                result = Integer.parseInt(sizeParam);
            } catch (NumberFormatException e) { /* ignore */ }
        }

        return result;
    }
}
