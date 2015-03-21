package com.fsck.k9.ui.messageview;


import android.app.PendingIntent;


interface OpenPgpHeaderViewCallback {
    void onPgpSignatureButtonClick(PendingIntent pendingIntent);
}
