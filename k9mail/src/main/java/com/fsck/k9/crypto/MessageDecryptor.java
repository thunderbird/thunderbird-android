package com.fsck.k9.crypto;


import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;


public class MessageDecryptor {
    private static final String MULTIPART_ENCRYPTED = "multipart/encrypted";

    public static List<Part> findEncryptedParts(Part startPart) {
        List<Part> encryptedParts = new ArrayList<Part>();
        Stack<Part> partsToCheck = new Stack<Part>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            String mimeType = part.getMimeType();
            Body body = part.getBody();

            if (MULTIPART_ENCRYPTED.equals(mimeType)) {
                encryptedParts.add(part);
            } else if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (int i = multipart.getCount() - 1; i >= 0; i--) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    partsToCheck.push(bodyPart);
                }
            }
        }

        return encryptedParts;
    }
}
