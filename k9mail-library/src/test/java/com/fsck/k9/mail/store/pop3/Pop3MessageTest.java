package com.fsck.k9.mail.store.pop3;


import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class Pop3MessageTest {

    @Test
    public void delete_setsDeletedFlag() throws MessagingException {
        Pop3Message message = new Pop3Message("001", mock(Pop3Folder.class));
        message.delete("Trash");

        assertTrue(message.getFlags().contains(Flag.DELETED));
    }
}
