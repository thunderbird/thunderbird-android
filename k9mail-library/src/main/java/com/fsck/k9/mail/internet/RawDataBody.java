package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.Body;


/**
 * See {@link MimeUtility#decodeBody(Body)}
 */
public interface RawDataBody extends Body {
    String getEncoding();
}
