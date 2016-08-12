package com.fsck.k9.message.extractors;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.support.annotation.WorkerThread;

import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.PartHeaderMetadata;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.DeferredFileBody;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalPart;
import com.fsck.k9.provider.AttachmentProvider;
import com.fsck.k9.provider.DecryptedFileProvider;


public class AttachmentInfoExtractor {
    private final Context context;


    public static AttachmentInfoExtractor getInstance() {
        Context context = Globals.getContext();
        return new AttachmentInfoExtractor(context);
    }

    @VisibleForTesting
    AttachmentInfoExtractor(Context context) {
        this.context = context;
    }

    @WorkerThread
    public List<AttachmentViewInfo> extractAttachmentInfoForView(List<Part> attachmentParts) {
        List<AttachmentViewInfo> attachments = new ArrayList<>();
        for (Part part : attachmentParts) {
            AttachmentViewInfo attachmentViewInfo = extractAttachmentInfo(part);
            if (!attachmentViewInfo.inlineAttachment) {
                attachments.add(attachmentViewInfo);
            }
        }

        return attachments;
    }

    @WorkerThread
    public AttachmentViewInfo extractAttachmentInfo(Part part) {
        Uri uri;
        long size;
        boolean isContentAvailable;

        if (part instanceof LocalPart) {
            LocalPart localPart = (LocalPart) part;
            String accountUuid = localPart.getAccountUuid();
            long messagePartId = localPart.getId();
            size = localPart.getSize();
            isContentAvailable = part.getBody() != null;
            uri = AttachmentProvider.getAttachmentUri(accountUuid, messagePartId);
        } else if (part instanceof LocalMessage) {
            LocalMessage localMessage = (LocalMessage) part;
            String accountUuid = localMessage.getAccount().getUuid();
            long messagePartId = localMessage.getMessagePartId();
            size = localMessage.getSize();
            isContentAvailable = part.getBody() != null;
            uri = AttachmentProvider.getAttachmentUri(accountUuid, messagePartId);
        } else {
            Body body = part.getBody();
            if (body instanceof DeferredFileBody) {
                DeferredFileBody decryptedTempFileBody = (DeferredFileBody) body;
                size = decryptedTempFileBody.getSize();
                uri = getDecryptedFileProviderUri(decryptedTempFileBody, PartHeaderMetadata.from(part).getMimeType());
                isContentAvailable = true;
            } else {
                throw new IllegalArgumentException("Unsupported part type provided");
            }
        }

        return extractAttachmentInfo(part, uri, size, isContentAvailable);
    }

    @Nullable
    @VisibleForTesting
    protected Uri getDecryptedFileProviderUri(DeferredFileBody decryptedTempFileBody, String mimeType) {
        Uri uri;
        try {
            File file = decryptedTempFileBody.getFile();
            uri = DecryptedFileProvider.getUriForProvidedFile(
                    context, file, decryptedTempFileBody.getEncoding(), mimeType);
        } catch (IOException e) {
            Log.e(K9.LOG_TAG, "Decrypted temp file (no longer?) exists!", e);
            uri = null;
        }
        return uri;
    }

    public AttachmentViewInfo extractAttachmentInfoForDatabase(Part part) throws MessagingException {
        boolean isContentAvailable = part.getBody() != null;
        return extractAttachmentInfo(part, Uri.EMPTY, AttachmentViewInfo.UNKNOWN_SIZE, isContentAvailable);
    }

    @WorkerThread
    private AttachmentViewInfo extractAttachmentInfo(Part part, Uri uri, long size, boolean isContentAvailable) {
        PartHeaderMetadata partHeaderMetadata = PartHeaderMetadata.from(part);

        String mimeType = partHeaderMetadata.getMimeType();
        String attachmentName = partHeaderMetadata.getDispositionFilename();

        if (attachmentName == null) {
            attachmentName = partHeaderMetadata.getContentTypeName();
        }

        if (attachmentName == null) {
            String extension = null;
            if (mimeType != null) {
                extension = MimeUtility.getExtensionByMimeType(mimeType);
            }
            attachmentName = "noname" + ((extension != null) ? "." + extension : "");
        }

        // Inline parts with a content-id are almost certainly components of an HTML message
        // not attachments. Only show them if the user pressed the button to show more
        // attachments.
        boolean inlineAttachment = false;
        String contentId = partHeaderMetadata.getContentId();
        if (partHeaderMetadata.isDispositionInline() && contentId != null) {
            inlineAttachment = true;
        }

        long attachmentSize = extractAttachmentSize(partHeaderMetadata, size);

        return new AttachmentViewInfo(mimeType, attachmentName, attachmentSize, uri,
                inlineAttachment, part, isContentAvailable, contentId);
    }

    @WorkerThread
    private long extractAttachmentSize(PartHeaderMetadata partHeaderMetadata, long explicitSize) {
        if (explicitSize != AttachmentViewInfo.UNKNOWN_SIZE) {
            return explicitSize;
        }

        Long sizeFromHeader = partHeaderMetadata.getDispositionSize();
        if (sizeFromHeader == null) {
            return AttachmentViewInfo.UNKNOWN_SIZE;
        }

        return sizeFromHeader;
    }
}
