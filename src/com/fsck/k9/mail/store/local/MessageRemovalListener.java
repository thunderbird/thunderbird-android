package com.fsck.k9.mail.store.local;

import com.fsck.k9.mail.Message;

public interface MessageRemovalListener {
    public void messageRemoved(Message message);
}
