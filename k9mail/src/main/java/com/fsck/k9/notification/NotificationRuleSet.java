package com.fsck.k9.notification;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;

/**
 * Describe a Notification rule set
 *
 */
public class NotificationRuleSet {
    private String mName;
    private String mSenderName;
    private String mSenderAddress;
    private String mSubject;
    private String mBody;
    private boolean mShouldNotify;

    public NotificationRuleSet() {
        mName = "";
        mSenderName = "";
        mSenderAddress = "";
        mSubject = "";
        mBody = "";
        mShouldNotify = true;
    }
    public synchronized String getName() { return mName; }

    public synchronized void setName(String name) { mName = name; }

    public synchronized String getSenderName() { return mSenderName; }

    public synchronized void setSenderName(String senderName) { mSenderName = senderName; }

    public synchronized String getSenderAddress() { return mSenderAddress; }

    public synchronized void setSenderAddress(String senderAddress) { mSenderAddress = senderAddress; }

    public synchronized String getSubject() { return mSubject; }

    public synchronized void setSubject(String subject) { mSubject = subject; }

    public synchronized String getBody() { return mBody; }

    public synchronized void setBody(String body) { mBody = body; }

    public synchronized boolean getShouldNotify() { return mShouldNotify; }

    public synchronized void setShouldNotify(boolean shouldNotify) { mShouldNotify = shouldNotify; }

    public synchronized boolean matches(String senderAddress, NotificationContent notificationContent) {

        // at least one rule must be set
        if (mSenderName.isEmpty() && mSenderAddress.isEmpty() && mSubject.isEmpty() && mBody.isEmpty() ) {
            return false;
        }

        if (!mSenderName.isEmpty() && !mSenderName.equalsIgnoreCase(notificationContent.sender)) {
            return false;
        }

        if (!mSenderAddress.isEmpty() && !mSenderAddress.equalsIgnoreCase(senderAddress)) {
            return false;
        }

        if (!mSubject.isEmpty() && !notificationContent.subject.contains(mSubject)) {
            return false;
        }

        String[] preview = notificationContent.preview.toString().split("\n");
        if (preview.length > 1) {
            if (!mBody.isEmpty() && !preview[1].contains(mBody)) {
                return false;
            }
        }
        return true;
    }
}