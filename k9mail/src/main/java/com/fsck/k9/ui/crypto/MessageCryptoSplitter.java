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
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError;


public class MessageCryptoSplitter {
    private MessageCryptoSplitter() { }

    @Nullable
    public static CryptoMessageParts split(@NonNull Message message, @Nullable MessageCryptoAnnotations annotations) {
        ArrayList<Part> extraParts = new ArrayList<>();
        Part primaryPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, extraParts);
        if (primaryPart == null) {
            return null;
        }

        if (annotations == null) {
            CryptoResultAnnotation rootPartAnnotation =
                    CryptoResultAnnotation.createErrorAnnotation(CryptoError.OPENPGP_ENCRYPTED_NO_PROVIDER, null);
            return new CryptoMessageParts(primaryPart, rootPartAnnotation, extraParts);
        }

        CryptoResultAnnotation rootPartAnnotation = annotations.get(primaryPart);
        Part rootPart;
        if (rootPartAnnotation != null && rootPartAnnotation.hasReplacementData()) {
            rootPart = rootPartAnnotation.getReplacementData();
        } else {
            rootPart = primaryPart;
        }

        return new CryptoMessageParts(rootPart, rootPartAnnotation, extraParts);
    }

    public static class CryptoMessageParts {
        public final Part contentPart;
        @Nullable
        public final CryptoResultAnnotation contentCryptoAnnotation;

        public final List<Part> extraParts;

        CryptoMessageParts(Part contentPart, @Nullable CryptoResultAnnotation contentCryptoAnnotation, List<Part> extraParts) {
            this.contentPart = contentPart;
            this.contentCryptoAnnotation = contentCryptoAnnotation;
            this.extraParts = Collections.unmodifiableList(extraParts);
        }
    }

}
