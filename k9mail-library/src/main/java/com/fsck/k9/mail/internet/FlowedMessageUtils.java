package com.fsck.k9.mail.internet;


import static com.fsck.k9.mail.internet.MimeUtility.getHeaderParameter;
import static com.fsck.k9.mail.internet.MimeUtility.isSameMimeType;


class FlowedMessageUtils {
    private static final String TEXT_PLAIN = "text/plain";
    private static final String HEADER_PARAM_FORMAT = "format";
    private static final String HEADER_FORMAT_FLOWED = "flowed";
    private static final String HEADER_PARAM_DELSP = "delsp";
    private static final String HEADER_DELSP_YES = "yes";


    static boolean isFormatFlowed(String contentType) {
        String mimeType = getHeaderParameter(contentType, null);
        if (isSameMimeType(TEXT_PLAIN, mimeType)) {
            String formatParameter = getHeaderParameter(contentType, HEADER_PARAM_FORMAT);
            return HEADER_FORMAT_FLOWED.equalsIgnoreCase(formatParameter);
        }
        return false;
    }

    static boolean isDelSp(String contentType) {
        if (isFormatFlowed(contentType)) {
            String delSpParameter = getHeaderParameter(contentType, HEADER_PARAM_DELSP);
            return HEADER_DELSP_YES.equalsIgnoreCase(delSpParameter);
        }
        return false;
    }
}
