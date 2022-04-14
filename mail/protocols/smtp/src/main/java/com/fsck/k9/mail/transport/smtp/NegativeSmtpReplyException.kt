package com.fsck.k9.mail.transport.smtp;


import android.text.TextUtils;

import com.fsck.k9.mail.MessagingException;


/**
 * Exception that is thrown when the server sends a negative reply (reply codes 4xx or 5xx).
 */
class NegativeSmtpReplyException extends MessagingException {
    private static final long serialVersionUID = 8696043577357897135L;


    private final int replyCode;
    private final String replyText;


    public NegativeSmtpReplyException(int replyCode, String replyText) {
        super(buildErrorMessage(replyCode, replyText), isPermanentSmtpError(replyCode));
        this.replyCode = replyCode;
        this.replyText = replyText;
    }

    private static String buildErrorMessage(int replyCode, String replyText) {
        return TextUtils.isEmpty(replyText) ? "Negative SMTP reply: " + replyCode : replyText;
    }

    private static boolean isPermanentSmtpError(int replyCode) {
        return replyCode >= 500 && replyCode <= 599;
    }

    public int getReplyCode() {
        return replyCode;
    }

    public String getReplyText() {
        return replyText;
    }
}
