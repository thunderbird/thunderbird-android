package com.fsck.k9.ui.messageview;


import android.app.PendingIntent;


interface SmimeHeaderViewCallback {
    void onSmimeSignatureButtonClick(PendingIntent pendingIntent);
}
