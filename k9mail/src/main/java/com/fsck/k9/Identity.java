package com.fsck.k9;

import java.io.Serializable;

public class Identity implements Serializable {
    private static final long serialVersionUID = -1666669071480985760L;

    private String description;
    private String name;
    private String email;
    private String signature;
    private boolean signatureUse;
    private String replyTo;

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized String getEmail() {
        return email;
    }

    public synchronized void setEmail(String email) {
        this.email = email;
    }

    public synchronized boolean getSignatureUse() {
        return signatureUse;
    }

    public synchronized void setSignatureUse(boolean signatureUse) {
        this.signatureUse = signatureUse;
    }

    public synchronized String getSignature() {
        return signature;
    }

    public synchronized void setSignature(String signature) {
        this.signature = signature;
    }

    public synchronized String getDescription() {
        return description;
    }

    public synchronized void setDescription(String description) {
        this.description = description;
    }

    public synchronized String getReplyTo() {
        return replyTo;
    }

    public synchronized void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public synchronized String toString() {
        return "Account.Identity(description=" + description + ", name=" + name + ", email=" + email + ", replyTo=" + replyTo + ", signature=" +
                signature;
    }
}
