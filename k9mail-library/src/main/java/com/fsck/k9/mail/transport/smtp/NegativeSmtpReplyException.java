package com.fsck.k9.mail.transport.smtp;


import com.fsck.k9.mail.MessagingException;


/**
 * Exception that is thrown when the server sends a negative reply (reply codes 4xx or 5xx).
 */
class NegativeSmtpReplyException extends MessagingException {
    private static final long serialVersionUID = 8696043577357897135L;

    private final int mReplyCode;
    private final String mReplyText;

    public NegativeSmtpReplyException(int replyCode, String replyText) {
        super((replyText != null && !replyText.isEmpty()) ?
                replyText : ("Negative SMTP reply: " + replyCode),
                isPermanentSmtpError(replyCode));
        mReplyCode = replyCode;
        mReplyText = replyText;
    }

    private static boolean isPermanentSmtpError(int replyCode) {
        return replyCode >= 500 && replyCode <= 599;
    }

    public int getReplyCode() {
        return mReplyCode;
    }

    public String getReplyText() {
        return mReplyText;
    }
}
