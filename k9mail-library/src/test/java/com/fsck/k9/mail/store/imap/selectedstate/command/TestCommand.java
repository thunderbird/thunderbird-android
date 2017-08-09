package com.fsck.k9.mail.store.imap.selectedstate.command;

class TestCommand extends FolderSelectedStateCommand {

    private TestCommand() {
    }

    @Override
    String createCommandString() {
        return createCombinedIdString().trim();
    }

    @Override
    Builder newBuilder() {
        return new Builder();
    }

    static class Builder extends FolderSelectedStateCommand.Builder<TestCommand, Builder> {

        @Override
        TestCommand createCommand() {
            return new TestCommand();
        }

        @Override
        Builder createBuilder() {
            return this;
        }
    }
}