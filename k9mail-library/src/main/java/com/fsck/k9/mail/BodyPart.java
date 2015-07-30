package com.fsck.k9.mail;


public abstract class BodyPart implements Part {
    private String serverExtra;
    private Multipart parent;

    @Override
    public String getServerExtra() {
        return serverExtra;
    }

    @Override
    public void setServerExtra(String serverExtra) {
        this.serverExtra = serverExtra;
    }

    public Multipart getParent() {
        return parent;
    }

    public void setParent(Multipart parent) {
        this.parent = parent;
    }

    public abstract void setEncoding(String encoding) throws MessagingException;
}
