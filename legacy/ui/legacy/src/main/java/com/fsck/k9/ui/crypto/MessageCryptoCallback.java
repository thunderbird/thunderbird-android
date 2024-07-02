package com.fsck.k9.ui.crypto;


import android.content.IntentSender;

import com.fsck.k9.mailstore.MessageCryptoAnnotations;


public interface MessageCryptoCallback {
    void onCryptoHelperProgress(int current, int max);
    void onCryptoOperationsFinished(MessageCryptoAnnotations annotations);
    boolean startPendingIntentForCryptoHelper(IntentSender intentSender, int requestCode);
}
