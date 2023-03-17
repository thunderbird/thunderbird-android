package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import java.util.TreeSet
import org.junit.Test

class ImapCommandSplitterTest {

    @Test
    fun splitCommand_withManyNonContiguousIds_shouldSplitCommand() {
        val ids = ImapResponseHelper.createNonContiguousIdSet(10000, 10500, 2)
        val groupedIds = GroupedIds(ids, emptyList())

        val commands = ImapCommandSplitter.splitCommand(COMMAND_PREFIX, COMMAND_SUFFIX, groupedIds, 980)

        assertThat(commands.size).isEqualTo(2)
        assertCommandLengthLimit(commands, 980)
        verifyCommandString(commands[0], ImapResponseHelper.createNonContiguousIdSet(10000, 10316, 2))
        verifyCommandString(commands[1], ImapResponseHelper.createNonContiguousIdSet(10318, 10500, 2))
    }

    @Test
    fun splitCommand_withContiguousAndNonContiguousIds_shouldGroupIdsAndSplitCommand() {
        val idSet: Set<Long> = ImapResponseHelper.createNonContiguousIdSet(10000, 10298, 2) +
            ImapResponseHelper.createNonContiguousIdSet(10402, 10500, 2)
        val idGroups = listOf(ContiguousIdGroup(10300L, 10400L))
        val groupedIds = GroupedIds(idSet, idGroups)

        val commands = ImapCommandSplitter.splitCommand(COMMAND_PREFIX, COMMAND_SUFFIX, groupedIds, 980)

        assertThat(commands.size).isEqualTo(2)
        assertCommandLengthLimit(commands, 980)
        verifyCommandString(
            commands[0],
            ImapResponseHelper.createNonContiguousIdSet(10000, 10298, 2) +
                ImapResponseHelper.createNonContiguousIdSet(10402, 10418, 2),
        )
        verifyCommandString(commands[1], ImapResponseHelper.createNonContiguousIdSet(10420, 10500, 2), "10300:10400")
    }

    @Test
    fun splitCommand_withEmptySuffix_shouldCreateCommandWithoutTrailingSpace() {
        val ids = ImapResponseHelper.createNonContiguousIdSet(1, 2, 1)
        val groupedIds = GroupedIds(ids, emptyList())

        val commands = ImapCommandSplitter.splitCommand("UID SEARCH UID", "", groupedIds, 980)

        assertThat(commands.size).isEqualTo(1)
        assertThat(commands[0]).isEqualTo("UID SEARCH UID 1,2")
    }

    private fun assertCommandLengthLimit(commands: List<String>, lengthLimit: Int) {
        for (command in commands) {
            assertThat(command.length, "Command is too long").isLessThanOrEqualTo(lengthLimit)
        }
    }

    private fun verifyCommandString(actualCommand: String, ids: Set<Long>, idGroupString: String? = null) {
        val sortedIds: Set<Long> = TreeSet(ids)
        val expectedCommandBuilder = StringBuilder(COMMAND_PREFIX)
            .append(" ")
            .append(ImapUtility.join(",", sortedIds))
        if (idGroupString != null) {
            expectedCommandBuilder.append(',').append(idGroupString)
        }
        expectedCommandBuilder.append(" ").append(COMMAND_SUFFIX)

        val expectedCommand = expectedCommandBuilder.toString()

        assertThat(actualCommand).isEqualTo(expectedCommand)
    }

    companion object {
        private const val COMMAND_PREFIX = "UID COPY"
        private const val COMMAND_SUFFIX = "\"Destination\""
    }
}
