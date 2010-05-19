package com.fsck.k9.controller;

import com.fsck.k9.mail.Message;

public interface MessageRemovalListener
{
    public void messageRemoved(Message message);
}
