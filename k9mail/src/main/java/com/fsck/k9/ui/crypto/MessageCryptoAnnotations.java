package com.fsck.k9.ui.crypto;


import java.util.HashMap;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.CryptoResultAnnotation;


public class MessageCryptoAnnotations {
    private HashMap<Part, CryptoResultAnnotation> annotations = new HashMap<>();

    MessageCryptoAnnotations() {
        // Package-private constructor
    }

    void put(Part part, CryptoResultAnnotation annotation) {
        annotations.put(part, annotation);
    }

    public CryptoResultAnnotation get(Part part) {
        return annotations.get(part);
    }

    public boolean has(Part part) {
        return annotations.containsKey(part);
    }
}
