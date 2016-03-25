package com.fsck.k9.ui.crypto;


import java.util.HashMap;
import java.util.List;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.CryptoResultAnnotation;


public class MessageCryptoAnnotations {
    private HashMap<Part, List<CryptoResultAnnotation>> annotations = new HashMap<>();

    MessageCryptoAnnotations() {
        // Package-private constructor
    }

    void put(Part part, List<CryptoResultAnnotation> annotation) {
        annotations.put(part, annotation);
    }

    public List<CryptoResultAnnotation> get(Part part) {
        return annotations.get(part);
    }

    public boolean has(Part part) {
        return annotations.containsKey(part);
    }
}
