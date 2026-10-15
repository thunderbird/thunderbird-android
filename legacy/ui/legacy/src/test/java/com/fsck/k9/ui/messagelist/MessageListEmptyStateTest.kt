package com.fsck.k9.ui.messagelist

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class MessageListEmptyStateTest {

    @Test
    fun `of should return no results for a search even if folders are unsynced`() {
        val result = MessageListEmptyState.of(isSearch = true, hasUnsyncedFolders = true, isPullToSyncAllowed = true)

        assertThat(result).isEqualTo(MessageListEmptyState.NO_RESULTS)
    }

    @Test
    fun `of should ask to pull down when folders are unsynced and pull to sync is allowed`() {
        val result = MessageListEmptyState.of(isSearch = false, hasUnsyncedFolders = true, isPullToSyncAllowed = true)

        assertThat(result).isEqualTo(MessageListEmptyState.NOT_SYNCED_PULL_TO_SYNC)
    }

    @Test
    fun `of should not ask to pull down when folders are unsynced but pull to sync isn't allowed`() {
        val result = MessageListEmptyState.of(isSearch = false, hasUnsyncedFolders = true, isPullToSyncAllowed = false)

        assertThat(result).isEqualTo(MessageListEmptyState.NOT_SYNCED)
    }

    @Test
    fun `of should return no messages when all folders are synced`() {
        val result = MessageListEmptyState.of(isSearch = false, hasUnsyncedFolders = false, isPullToSyncAllowed = true)

        assertThat(result).isEqualTo(MessageListEmptyState.NO_MESSAGES)
    }
}
