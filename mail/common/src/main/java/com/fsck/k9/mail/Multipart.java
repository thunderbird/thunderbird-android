
package com.fsck.k9.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.james.mime4j.util.MimeUtil;
import net.thunderbird.core.common.exception.MessagingException;

public abstract class Multipart implements Body {
    private Part mParent;

    private final List<BodyPart> mParts = new ArrayList<>();

    public void addBodyPart(BodyPart part) {
        mParts.add(part);
        part.setParent(this);
    }

    public BodyPart getBodyPart(int index) {
        return mParts.get(index);
    }

    public List<BodyPart> getBodyParts() {
        return Collections.unmodifiableList(mParts);
    }

    public abstract String getMimeType();

    public abstract String getBoundary();

    public int getCount() {
        return mParts.size();
    }

    public Part getParent() {
        return mParent;
    }

    public void setParent(Part parent) {
        this.mParent = parent;
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        if (!MimeUtil.ENC_7BIT.equalsIgnoreCase(encoding)
                && !MimeUtil.ENC_8BIT.equalsIgnoreCase(encoding)) {
            throw new MessagingException("Incompatible content-transfer-encoding for a multipart/* body");
        }

        /* Nothing else to do.  Each subpart has its own separate encoding */
    }

    public abstract byte[] getPreamble();
    public abstract byte[] getEpilogue();
}
