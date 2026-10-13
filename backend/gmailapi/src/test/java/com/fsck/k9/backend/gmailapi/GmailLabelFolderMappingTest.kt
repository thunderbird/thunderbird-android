package com.fsck.k9.backend.gmailapi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailLabelFolderMappingTest {
    @Test
    fun `chat and category system labels are not mapped to folders`() {
        val excludedLabelIds = listOf(
            "CHAT",
            "CATEGORY_PERSONAL",
            "CATEGORY_SOCIAL",
            "CATEGORY_PROMOTIONS",
            "CATEGORY_UPDATES",
            "CATEGORY_FORUMS",
            "CATEGORY_OTHER",
        )

        excludedLabelIds.forEach { labelId ->
            val testSubject = shouldMapGmailLabelToFolder(
                GmailLabel(id = labelId, name = labelId, type = "system"),
            )

            assertFalse("Expected $labelId not to map to a folder", testSubject)
        }
    }

    @Test
    fun `real system folders remain mapped`() {
        val systemFolderIds = listOf("INBOX", "SENT", "DRAFT", "TRASH", "SPAM", "ALL_MAIL")

        systemFolderIds.forEach { labelId ->
            val testSubject = shouldMapGmailLabelToFolder(
                GmailLabel(id = labelId, name = labelId, type = "system"),
            )

            assertTrue("Expected $labelId to map to a folder", testSubject)
        }
    }

    @Test
    fun `user-created labels remain mapped`() {
        val testSubject = shouldMapGmailLabelToFolder(
            GmailLabel(id = "Label_123", name = "Project updates", type = "user"),
        )

        assertTrue(testSubject)
    }
}
