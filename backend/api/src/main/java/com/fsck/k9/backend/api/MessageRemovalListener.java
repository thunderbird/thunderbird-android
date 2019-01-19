package com.fsck.k9.backend.api;

import com.fsck.k9.mail.Message;

public interface MessageRemovalListener {
    void messageRemoved(Message message);
}
