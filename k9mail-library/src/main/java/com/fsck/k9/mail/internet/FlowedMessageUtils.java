package com.fsck.k9.mail.internet;


import static com.fsck.k9.mail.internet.MimeUtility.getHeaderParameter;
import static com.fsck.k9.mail.internet.MimeUtility.isFormatFlowed;


public class FlowedMessageUtils {
    private static final String HEADER_PARAM_DELSP = "delsp";
    private static final String HEADER_DELSP_YES = "yes";


    static boolean isDelSp(String contentType) {
        if (isFormatFlowed(contentType)) {
            String delSpParameter = getHeaderParameter(contentType, HEADER_PARAM_DELSP);
            return HEADER_DELSP_YES.equalsIgnoreCase(delSpParameter);
        }
        return false;
    }
}
