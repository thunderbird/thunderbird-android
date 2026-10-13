package com.fsck.k9.search

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import org.junit.Test

class LocalSearchExtensionsTest {

    @Test
    fun `unifiedFolderRole should return the role of a unified search`() {
        val search = LocalMessageSearch.createUnifiedSearch(FolderRole.SENT)

        assertThat(search.unifiedFolderRole).isEqualTo(FolderRole.SENT)
    }

    @Test
    fun `unifiedFolderRole should return null for an account search`() {
        val search = LocalMessageSearch()

        assertThat(search.unifiedFolderRole).isNull()
    }
}
