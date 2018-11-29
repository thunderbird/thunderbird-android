package com.fsck.k9.ui.messageview;

import android.net.Uri;

import com.fsck.k9.mailstore.AttachmentViewInfo;

interface DownloadImageCallback {
    void onSaveImage(Uri imageUri);
}
