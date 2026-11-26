package com.fsck.k9.ui.messagelist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import org.junit.Test

class UnifiedFolderTitleTest : RobolectricTest() {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `should name each unified folder after its special folder`() {
        val titles = FolderRole.entries.associateWith { role ->
            context.getString(LocalMessageSearch.createUnifiedSearch(role).unifiedFolderTitleResId)
        }

        assertThat(titles).isEqualTo(
            mapOf(
                FolderRole.INBOX to "Unified Inbox",
                FolderRole.OUTBOX to "Unified Outbox",
                FolderRole.SENT to "Unified Sent",
                FolderRole.TRASH to "Unified Trash",
                FolderRole.DRAFTS to "Unified Drafts",
                FolderRole.ARCHIVE to "Unified Archive",
                FolderRole.SPAM to "Unified Spam",
            ),
        )
    }
}
