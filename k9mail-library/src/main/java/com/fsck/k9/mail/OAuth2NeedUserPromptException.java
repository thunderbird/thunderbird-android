package com.fsck.k9.mail;

public class OAuth2NeedUserPromptException extends MessagingException {

    public OAuth2NeedUserPromptException() {
        super("Need user's prompt for xoauth2");
    }
}
