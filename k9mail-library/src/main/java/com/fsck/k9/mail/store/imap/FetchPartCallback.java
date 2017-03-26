package com.fsck.k9.mail.store.imap;


import java.io.IOException;

import com.fsck.k9.mail.AttachmentProgressCallback;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.filter.FixedLengthInputStream;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;


class FetchPartCallback implements ImapResponseCallback {
    private Part mPart;
    private AttachmentProgressCallback attachmentProgressCallback;

    FetchPartCallback(Part part) {
        mPart = part;
    }

    FetchPartCallback(Part part, AttachmentProgressCallback attachmentProgressCallback) {
        mPart = part;
        this.attachmentProgressCallback = attachmentProgressCallback;
    }

    @Override
    public Object foundLiteral(ImapResponse response, FixedLengthInputStream literal) throws IOException {
        if (response.getTag() == null &&
                ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH")) {
            //TODO: check for correct UID

            String contentTransferEncoding = mPart
                    .getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
            String contentType = mPart
                    .getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0];

            if (attachmentProgressCallback != null) {
                return MimeUtility.createBody(literal, contentTransferEncoding, contentType, attachmentProgressCallback);
            }

            return MimeUtility.createBody(literal, contentTransferEncoding, contentType, null);
        }
        return null;
    }
}
