package com.fsck.k9.mailstore;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;


/**
 * This class is used to encapsulate a message part, providing an interface to
 * get relevant info for a given Content-ID URI.
 *
 * The point of this class is to keep the Content-ID loading code agnostic of
 * the underlying part structure.
 */
public class AttachmentResolver {
    Map<String,Uri> contentIdToAttachmentUriMap;


    private AttachmentResolver(Map<String, Uri> contentIdToAttachmentUriMap) {
        this.contentIdToAttachmentUriMap = contentIdToAttachmentUriMap;
    }

    @Nullable
    public Uri getAttachmentUriForContentId(String cid) {
        return contentIdToAttachmentUriMap.get(cid);
    }

    @WorkerThread
    public static AttachmentResolver createFromPart(Context context, Part part) {
        Map<String,Uri> contentIdToAttachmentUriMap = buildCidToAttachmentUriMap(context, part);

        return new AttachmentResolver(contentIdToAttachmentUriMap);
    }

    private static Map<String,Uri> buildCidToAttachmentUriMap(Context context, Part rootPart) {
        HashMap<String,Uri> result = new HashMap<>();

        Stack<Part> partsToCheck = new Stack<>();
        partsToCheck.push(rootPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();

            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (Part bodyPart : multipart.getBodyParts()) {
                    partsToCheck.push(bodyPart);
                }
            } else {
                try {
                    String contentId = part.getContentId();
                    if (contentId != null) {
                        AttachmentViewInfo attachmentInfo = AttachmentInfoExtractor.extractAttachmentInfo(context, part);
                        result.put(contentId, attachmentInfo.uri);
                    }
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Error extracting attachment info", e);
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

}
