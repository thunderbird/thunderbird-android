package com.fsck.k9.view;


import android.content.Context;
import android.util.AttributeSet;


public class MessageCryptoStatusView extends ToolableViewAnimator {
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_SIGN_3_OK = 1;
    private static final int STATUS_SIGN_2_WARNING = 2;
    private static final int STATUS_SIGN_1_ERROR = 3;
    private static final int STATUS_SIGN_0_UNKNOWN = 4;
    private static final int STATUS_LOCK_3_OK = 5;
    private static final int STATUS_LOCK_2_WARNING = 6;
    private static final int STATUS_LOCK_1_ERROR = 7;
    private static final int STATUS_LOCK_0_UNKNOWN = 8;
    private static final int STATUS_LOCK_UNKNOWN = 9;
    private static final int STATUS_LOCK_ERROR = 10;


    public MessageCryptoStatusView(Context context) {
        super(context);
    }

    public MessageCryptoStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageCryptoStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCryptoDisplayStatus(MessageCryptoDisplayStatus displayStatus) {
        int whichChild = displayStatusToChildIndex(displayStatus);
        setDisplayedChild(whichChild);
    }

    private int displayStatusToChildIndex(MessageCryptoDisplayStatus displayStatus) {
        switch (displayStatus) {
            case DISABLED:
                return STATUS_DISABLED;

            case UNENCRYPTED_SIGN_UNKNOWN:
                return STATUS_SIGN_0_UNKNOWN;
            case UNENCRYPTED_SIGN_VERIFIED:
                return STATUS_SIGN_3_OK;
            case UNENCRYPTED_SIGN_UNVERIFIED:
                return STATUS_SIGN_2_WARNING;
            case UNENCRYPTED_SIGN_ERROR:
                return STATUS_SIGN_1_ERROR;
            case UNENCRYPTED_SIGN_MISMATCH:
            case UNENCRYPTED_SIGN_EXPIRED:
            case UNENCRYPTED_SIGN_REVOKED:
            case UNENCRYPTED_SIGN_INSECURE:
                return STATUS_SIGN_1_ERROR;

            case ENCRYPTED_SIGN_UNKNOWN:
                return STATUS_LOCK_0_UNKNOWN;
            case ENCRYPTED_SIGN_VERIFIED:
                return STATUS_LOCK_3_OK;
            case ENCRYPTED_SIGN_UNVERIFIED:
                return STATUS_LOCK_2_WARNING;
            case ENCRYPTED_SIGN_ERROR:
            case ENCRYPTED_SIGN_MISMATCH:
            case ENCRYPTED_SIGN_EXPIRED:
            case ENCRYPTED_SIGN_REVOKED:
            case ENCRYPTED_SIGN_INSECURE:
                return STATUS_LOCK_1_ERROR;

            case ENCRYPTED_ERROR:
                return STATUS_LOCK_ERROR;
            case ENCRYPTED_UNSIGNED:
                return STATUS_LOCK_UNKNOWN;
        }

        throw new AssertionError("all cases must be handled, this is a bug!");
    }
}
