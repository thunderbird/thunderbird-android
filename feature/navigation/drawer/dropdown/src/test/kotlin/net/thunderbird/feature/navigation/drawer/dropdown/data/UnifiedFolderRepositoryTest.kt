package net.thunderbird.feature.navigation.drawer.dropdown.data

import app.k9mail.legacy.message.controller.MessageCounts
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.UnifiedFolderSelection

internal class UnifiedFolderRepositoryTest {

    @Test
    fun `should return DisplayUnifiedFolder for unified inbox`() = runTest {
        val messageCountsProvider = FakeMessageCountsProvider(
            messageCounts = MessageCounts(
                unread = 2,
                starred = 2,
            ),
        )
        val testSubject = UnifiedFolderRepository(
            messageCountsProvider = messageCountsProvider,
        )
        val result = testSubject.getUnifiedDisplayFolderFlow(FolderRole.INBOX).first()

        assertThat(result).isEqualTo(
            UnifiedDisplayFolder(
                role = FolderRole.INBOX,
                unreadMessageCount = 2,
                starredMessageCount = 2,
            ),
        )

        val search = messageCountsProvider.recordedSearch
        assertThat(search.id).isEqualTo(UnifiedAccountId)
        assertThat(search.type).isEqualTo(
            LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(FolderRole.INBOX)),
        )
        // The folder is chosen per account when the search runs
        assertThat(search.leafSet).isEmpty()
    }

    @Test
    fun `should search the folder with the matching role in each account`() = runTest {
        val messageCountsProvider = FakeMessageCountsProvider(messageCounts = MessageCounts(unread = 0, starred = 0))
        val testSubject = UnifiedFolderRepository(messageCountsProvider = messageCountsProvider)

        val searchTypes = FolderRole.entries.associateWith { role ->
            testSubject.getUnifiedDisplayFolderFlow(role).first()
            messageCountsProvider.recordedSearch.type
        }

        assertThat(searchTypes).isEqualTo(
            mapOf(
                FolderRole.INBOX to unifiedSearchType(FolderRole.INBOX),
                FolderRole.OUTBOX to unifiedSearchType(FolderRole.OUTBOX),
                FolderRole.DRAFTS to unifiedSearchType(FolderRole.DRAFTS),
                FolderRole.SENT to unifiedSearchType(FolderRole.SENT),
                FolderRole.ARCHIVE to unifiedSearchType(FolderRole.ARCHIVE),
                FolderRole.SPAM to unifiedSearchType(FolderRole.SPAM),
                FolderRole.TRASH to unifiedSearchType(FolderRole.TRASH),
            ),
        )
    }

    private fun unifiedSearchType(role: FolderRole): LocalMessageSearchType {
        return LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(role))
    }
}
