package com.fsck.k9.message;


import java.util.List;

import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class ComposePgpEnableByDefaultDecider {
    public boolean shouldEncryptByDefault(Message localMessage) {
        return messageIsEncrypted(localMessage);
    }

    private boolean messageIsEncrypted(Message localMessage) {
        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(localMessage);
        return !encryptedParts.isEmpty();
    }
}
