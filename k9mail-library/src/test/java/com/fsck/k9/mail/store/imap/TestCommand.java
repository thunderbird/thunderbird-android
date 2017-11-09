package com.fsck.k9.mail.store.imap;

import java.util.Collections;
import java.util.List;
import java.util.Set;


class TestCommand extends FolderSelectedStateCommand<SelectedStateResponse> {

    private TestCommand(Set<Long> ids) {
        super(ids);
    }

    @Override
    String createCommandString() {
        return String.format("TEST %s", createCombinedIdString()).trim();
    }

    @Override
    public SelectedStateResponse parseResponses(List<ImapResponse> unparsedResponses) {
        return null;
    }

    static TestCommand createWithIdSetAndGroup(Set<Long> ids, Long start, Long end) {
        TestCommand command = new TestCommand(ids);
        command.addIdGroup(start, end);
        return command;
    }

    static TestCommand createWithIdSet(Set<Long> ids) {
        return createWithIdSetAndGroup(ids, null, null);
    }

    static TestCommand createWithIdGroup(long start, long end) {
        return createWithIdSetAndGroup(Collections.<Long>emptySet(), start, end);
    }
}
