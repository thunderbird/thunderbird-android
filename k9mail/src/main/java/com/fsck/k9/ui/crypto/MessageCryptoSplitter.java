package com.fsck.k9.ui.crypto;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.CryptoResultAnnotation;


public class MessageCryptoSplitter {
    private MessageCryptoSplitter() { }

    @Nullable
    public static CryptoMessageParts split(@NonNull Message message, @Nullable MessageCryptoAnnotations annotations) {
        if (annotations == null) {
            return null;
        }

        ArrayList<Part> extraParts = new ArrayList<>();
        Part primaryPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, extraParts);

        if (!annotations.has(primaryPart)) {
            return null;
        }

        CryptoResultAnnotation rootPartAnnotation = annotations.get(primaryPart);
        Part rootPart;
        if (rootPartAnnotation.hasReplacementData()) {
            rootPart = rootPartAnnotation.getReplacementData();
        } else {
            rootPart = primaryPart;
        }

        return new CryptoMessageParts(rootPart, rootPartAnnotation, extraParts);
    }

    public static class CryptoMessageParts {
        public final Part contentPart;
        public final CryptoResultAnnotation contentCryptoAnnotation;

        public final List<Part> extraParts;

        CryptoMessageParts(Part contentPart, CryptoResultAnnotation contentCryptoAnnotation, List<Part> extraParts) {
            this.contentPart = contentPart;
            this.contentCryptoAnnotation = contentCryptoAnnotation;
            this.extraParts = Collections.unmodifiableList(extraParts);
        }
    }

}
