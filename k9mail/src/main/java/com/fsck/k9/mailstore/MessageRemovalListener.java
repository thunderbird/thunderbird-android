package com.fsck.k9.mailstore;

import com.fsck.k9.mail.Message;

public interface MessageRemovalListener {
    void messageRemoved(Message message);
}
