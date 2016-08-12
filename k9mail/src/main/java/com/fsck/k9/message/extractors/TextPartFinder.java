package com.fsck.k9.message.extractors;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.PartHeaderMetadata;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;


class TextPartFinder {
    @Nullable
    public Part findFirstTextPart(@NonNull Part part) {
        Body body = part.getBody();

        if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            if (PartHeaderMetadata.from(part).isMimeType("multipart/alternative")) {
                return findTextPartInMultipartAlternative(multipart);
            } else {
                return findTextPartInMultipart(multipart);
            }
        } else if (PartHeaderMetadata.from(part).isMimeTypeAnyOf("text/plain", "text/html")) {
            return part;
        }

        return null;
    }

    private Part findTextPartInMultipartAlternative(Multipart multipart) {
        Part htmlPart = null;

        for (BodyPart bodyPart : multipart.getBodyParts()) {
            PartHeaderMetadata fancyBodyPart = PartHeaderMetadata.from(bodyPart);
            Body body = bodyPart.getBody();

            if (body instanceof Multipart) {
                Part candidatePart = findFirstTextPart(bodyPart);
                if (candidatePart != null) {
                    if (PartHeaderMetadata.from(candidatePart).isMimeType("text/html")) {
                        htmlPart = candidatePart;
                    } else {
                        return candidatePart;
                    }
                }
            } else if (fancyBodyPart.isMimeType("text/plain")) {
                return bodyPart;
            } else if (fancyBodyPart.isMimeType("text/html") && htmlPart == null) {
                htmlPart = bodyPart;
            }
        }

        if (htmlPart != null) {
            return htmlPart;
        }

        return null;
    }

    private Part findTextPartInMultipart(Multipart multipart) {
        for (BodyPart bodyPart : multipart.getBodyParts()) {
            Body body = bodyPart.getBody();

            if (body instanceof Multipart) {
                Part candidatePart = findFirstTextPart(bodyPart);
                if (candidatePart != null) {
                    return candidatePart;
                }
            } else if (PartHeaderMetadata.from(bodyPart).isMimeTypeAnyOf("text/plain", "text/html")) {
                return bodyPart;
            }
        }

        return null;
    }
}
