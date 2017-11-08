package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.util.Collections;

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
