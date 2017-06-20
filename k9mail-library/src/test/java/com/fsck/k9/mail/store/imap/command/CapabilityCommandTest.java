package com.fsck.k9.mail.store.imap.command;


import com.fsck.k9.mail.store.imap.ImapConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class CapabilityCommandTest {

    private ImapCommandFactory commandFactory;
    private ImapConnection connection;

    @Before
    public void setUp() {
        connection = Mockito.mock(ImapConnection.class);
        commandFactory = ImapCommandFactory.create(connection, null);
    }

    @Test
    public void executeInternal_shouldIssueProperCommand() throws Exception {
        CapabilityCommand command = commandFactory.createCapabilityCommand();

        command.executeInternal();

        Mockito.verify(connection).executeSimpleCommand("CAPABILITY", false);
    }
}
