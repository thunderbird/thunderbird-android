package com.fsck.k9.mail.store.imap;


import java.util.Collections;

import com.fsck.k9.mail.store.imap.UidCopyCommand;
import org.junit.Assert;
import org.junit.Test;


public class UidCopyCommandTest {

    @Test
    public void createCommandString_shouldCreateExpectedString() {
        UidCopyCommand command = UidCopyCommand.createWithUids(Collections.singleton(1L),
                "\"Destination\"");

        String commandString = command.createCommandString();

        Assert.assertEquals(commandString, "UID COPY 1 \"Destination\"");
    }
}
