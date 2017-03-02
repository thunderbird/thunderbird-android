package com.fsck.k9.mail;

public interface AttachmentProgressCallback {

    /*
     * Used to update the AttachmentDownloadDialogFragment.
     */

    void onUpdate(int progress);
}
