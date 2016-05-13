package com.fsck.k9.message.extractors;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.DecryptedTempFileBody;
import com.fsck.k9.mailstore.LocalPart;
import com.fsck.k9.provider.AttachmentProvider;
import com.fsck.k9.provider.K9FileProvider;


public class AttachmentInfoExtractor {
    public static List<AttachmentViewInfo> extractAttachmentInfos(Context context, List<Part> attachmentParts)
            throws MessagingException {

        List<AttachmentViewInfo> attachments = new ArrayList<>();
        for (Part part : attachmentParts) {
            attachments.add(extractAttachmentInfo(context, part));
        }

        return attachments;
    }

    public static AttachmentViewInfo extractAttachmentInfo(Context context, Part part) throws MessagingException {
        if (part instanceof LocalPart) {
            LocalPart localPart = (LocalPart) part;
            String accountUuid = localPart.getAccountUuid();
            long messagePartId = localPart.getId();
            String mimeType = part.getMimeType();
            String displayName = localPart.getDisplayName();
            long size = localPart.getSize();
            boolean firstClassAttachment = localPart.isFirstClassAttachment();
            Uri uri = AttachmentProvider.getAttachmentUri(accountUuid, messagePartId);

            return new AttachmentViewInfo(mimeType, displayName, size, uri, firstClassAttachment, part);
        } else {
            Body body = part.getBody();
            if (body instanceof DecryptedTempFileBody) {
                DecryptedTempFileBody decryptedTempFileBody = (DecryptedTempFileBody) body;
                long size = decryptedTempFileBody.getSize();

                Uri uri;
                try {
                    File file = decryptedTempFileBody.getFile();
                    uri = K9FileProvider.getUriForFile(context, file, part.getMimeType());
                } catch (IOException e) {
                    throw new MessagingException("Error preparing decrypted data as attachment", e);
                }

                return extractAttachmentInfo(part, uri, size);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    public static AttachmentViewInfo extractAttachmentInfo(Part part) throws MessagingException {
        return extractAttachmentInfo(part, Uri.EMPTY, AttachmentViewInfo.UNKNOWN_SIZE);
    }

    private static AttachmentViewInfo extractAttachmentInfo(Part part, Uri uri, long size) throws MessagingException {
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
            String extension = MimeUtility.getExtensionByMimeType(mimeType);
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

    private static long extractAttachmentSize(String contentDisposition, long size) {
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
