package com.fsck.k9.message.extractors;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.ical.ICalParser;
import com.fsck.k9.ical.ICalPart;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.DeferredFileBody;
import com.fsck.k9.mailstore.ICalendarViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalPart;
import com.fsck.k9.provider.AttachmentProvider;
import com.fsck.k9.provider.DecryptedFileProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ICalendarInfoExtractor {
    private final Context context;


    public static ICalendarInfoExtractor getInstance() {
        Context context = Globals.getContext();
        return new ICalendarInfoExtractor(context);
    }

    @VisibleForTesting
    ICalendarInfoExtractor(Context context) {
        this.context = context;
    }

    @WorkerThread
    public List<ICalendarViewInfo> extractICalendarInfoForView(List<ICalPart> iCalParts)
            throws MessagingException {

        List<ICalendarViewInfo> calendarViewInfos = new ArrayList<>();
        for (ICalPart part : iCalParts) {
            calendarViewInfos.add(extractICalendarInfo(part));
        }

        return calendarViewInfos;
    }


    //TODO: Some of this is probably needed in reality

    /**
    @WorkerThread
    public ICalendarViewInfo extractICalendarInfo(Part part) throws MessagingException {
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
                uri = getDecryptedFileProviderUri(decryptedTempFileBody, part.getMimeType());
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

     **/

    @WorkerThread
    private ICalendarViewInfo extractICalendarInfo(ICalPart part)
            throws MessagingException {

        Uri uri;
        long size;
        boolean isContentAvailable;
        Part underlyingPart = part.getPart();

        if (underlyingPart instanceof LocalPart) {
            LocalPart localPart = (LocalPart) underlyingPart;
            String accountUuid = localPart.getAccountUuid();
            long messagePartId = localPart.getPartId();
            size = localPart.getSize();
            isContentAvailable = underlyingPart.getBody() != null;
            uri = AttachmentProvider.getAttachmentUri(accountUuid, messagePartId);
        } else if (underlyingPart instanceof LocalMessage) {
            LocalMessage localMessage = (LocalMessage) underlyingPart;
            String accountUuid = localMessage.getAccount().getUuid();
            long messagePartId = localMessage.getMessagePartId();
            size = localMessage.getSize();
            isContentAvailable = underlyingPart.getBody() != null;
            uri = AttachmentProvider.getAttachmentUri(accountUuid, messagePartId);
        } else {
            Body body = underlyingPart.getBody();
            if (body instanceof DeferredFileBody) {
                DeferredFileBody decryptedTempFileBody = (DeferredFileBody) body;
                size = decryptedTempFileBody.getSize();
                uri = getDecryptedFileProviderUri(decryptedTempFileBody, underlyingPart.getMimeType());
                isContentAvailable = true;
            } else {
                throw new IllegalArgumentException("Unsupported part type provided");
            }
        }

        return new ICalendarViewInfo(
                underlyingPart, isContentAvailable,
                size,
                uri,
                ICalParser.parse(part));
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

    @WorkerThread
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
