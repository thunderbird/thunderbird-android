package net.thunderbird.feature.navigation.drawer.dropdown.data

import app.k9mail.legacy.message.controller.MessageCounts
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.UnifiedFolderSelection
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute

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
        val folderType = UnifiedDisplayFolderType.INBOX

        val result = testSubject.getUnifiedDisplayFolderFlow(folderType).first()

        assertThat(result).isEqualTo(
            UnifiedDisplayFolder(
                folderId = "unified_inbox",
                unifiedType = folderType,
                unreadMessageCount = 2,
                starredMessageCount = 2,
            ),
        )

        val search = messageCountsProvider.recordedSearch
        assertThat(search.id).isEqualTo(UnifiedAccountId)
        assertThat(search.type).isEqualTo(
            LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(FolderType.INBOX)),
        )
        val condition = search.conditions.condition
        assertThat(condition?.value).isEqualTo("1")
        assertThat(condition?.attribute).isEqualTo(SearchAttribute.EQUALS)
        assertThat(condition?.field).isEqualTo(MessageSearchField.INTEGRATE)
    }
}
