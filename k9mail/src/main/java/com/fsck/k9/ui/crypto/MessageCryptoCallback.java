package com.fsck.k9.ui.crypto;


import com.fsck.k9.ui.messageview.MessageCryptoHelper.MessageCryptoAnnotations;


public interface MessageCryptoCallback {
    void onCryptoOperationsFinished(MessageCryptoAnnotations annotations);
}
