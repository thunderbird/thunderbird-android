package com.fsck.k9.mailstore;


import android.net.Uri;

import com.fsck.k9.mail.Part;


public class AttachmentViewInfo {
    public static final long UNKNOWN_SIZE = -1;

    public final String mimeType;
    public final String displayName;
    public final long size;

    /**
     * A content provider URI that can be used to retrieve the decoded attachment.
     * <p/>
     * Note: All content providers must support an alternative MIME type appended as last URI segment.
     *
     * @see com.fsck.k9.ui.messageview.AttachmentController#getAttachmentUriForMimeType(AttachmentViewInfo, String)
     */
    public final Uri uri;
    public final boolean firstClassAttachment;
    public final Part part;

    public AttachmentViewInfo(String mimeType, String displayName, long size, Uri uri, boolean firstClassAttachment,
            Part part) {
        this.mimeType = mimeType;
        this.displayName = displayName;
        this.size = size;
        this.uri = uri;
        this.firstClassAttachment = firstClassAttachment;
        this.part = part;
    }
}
