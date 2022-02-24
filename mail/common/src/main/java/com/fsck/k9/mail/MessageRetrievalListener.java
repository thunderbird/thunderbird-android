
package com.fsck.k9.mail;


public interface MessageRetrievalListener<T extends Message> {
    void messageFinished(T message);
}
