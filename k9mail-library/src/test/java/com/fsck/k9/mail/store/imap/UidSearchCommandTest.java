package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.Date;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.store.imap.UidSearchCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;



public class UidSearchCommandTest {

    private static final String QUERY = "query";
    private static final String MESSAGE_ID = "<00000000.0000000@example.org>";
    private static final Date SINCE = new Date(10000000000L);
    private static final Set<Flag> REQUIRED_FLAGS = Collections.singleton(Flag.FLAGGED);
    private static final Set<Flag> FORBIDDEN_FLAGS = Collections.singleton(Flag.DELETED);

    private static final String SEARCH_KEY_QUERY_WITH_FULL_TEXT_SEARCH_ENABLED = "TEXT \"query\"";
    private static final String SEARCH_KEY_QUERY_WITH_FULL_TEXT_SEARCH_DISABLED = "OR SUBJECT \"query\" FROM \"query\"";
    private static final String SEARCH_KEY_MESSAGE_ID = "HEADER MESSAGE-ID \"<00000000.0000000@example.org>\"";
    private static final String SEARCH_KEY_SINCE = "SINCE 26-Apr-1970";
    private static final String SEARCH_KEY_REQUIRED_FLAGS = "FLAGGED";
    private static final String SEARCH_KEY_FORBIDDEN_FLAGS = "NOT DELETED";

    @Test
    public void createCommandString_withFullTextSearchEnabled_shouldCreateStringContainingAllTerms() throws Exception {
        UidSearchCommand command = createUidSearchCommand(true);

        String commandString = command.createCommandString();

        assertTrue(commandString.contains(SEARCH_KEY_QUERY_WITH_FULL_TEXT_SEARCH_ENABLED));
        testCommandString(commandString);
    }

    @Test
    public void createCommandString_withFullTextSearchDisabled_shouldCreateStringContainingAllTerms() throws Exception {
        UidSearchCommand command = createUidSearchCommand(false);

        String commandString = command.createCommandString();

        assertTrue(commandString.contains(SEARCH_KEY_QUERY_WITH_FULL_TEXT_SEARCH_DISABLED));
        testCommandString(commandString);
    }

    private UidSearchCommand createUidSearchCommand(boolean performFullTextSearch) {
        return new UidSearchCommand.Builder()
                .performFullTextSearch(performFullTextSearch)
                .queryString(QUERY)
                .messageId(MESSAGE_ID)
                .since(SINCE)
                .requiredFlags(REQUIRED_FLAGS)
                .forbiddenFlags(FORBIDDEN_FLAGS)
                .build();
    }

    private void testCommandString(String commandString) {
        assertTrue(commandString.contains(SEARCH_KEY_MESSAGE_ID));
        assertTrue(commandString.contains(SEARCH_KEY_SINCE));
        assertTrue(commandString.contains(SEARCH_KEY_REQUIRED_FLAGS));
        assertTrue(commandString.contains(SEARCH_KEY_FORBIDDEN_FLAGS));
    }
}
