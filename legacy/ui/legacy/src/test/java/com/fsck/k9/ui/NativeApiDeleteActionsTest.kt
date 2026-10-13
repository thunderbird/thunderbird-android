package com.fsck.k9.ui

import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeApiDeleteActionsTest {
    @Test
    fun `Gmail Trash is permanent delete but Gmail inbox is not`() {
        assertTrue(
            isPermanentDeleteFolder(
                Protocols.GMAIL_API,
                trashFolderId = 10L,
                spamFolderId = 11L,
                folderId = 10L,
            ),
        )
        assertFalse(
            isPermanentDeleteFolder(
                Protocols.GMAIL_API,
                trashFolderId = 10L,
                spamFolderId = 11L,
                folderId = 12L,
            ),
        )
    }

    @Test
    fun `Zoho Spam or Junk is permanent delete but Zoho Trash is not`() {
        assertTrue(
            isPermanentDeleteFolder(
                Protocols.ZOHO_MAIL_API,
                trashFolderId = 20L,
                spamFolderId = 21L,
                folderId = 21L,
            ),
        )
        assertFalse(
            isPermanentDeleteFolder(
                Protocols.ZOHO_MAIL_API,
                trashFolderId = 20L,
                spamFolderId = 21L,
                folderId = 20L,
            ),
        )
    }

    @Test
    fun `legacy IMAP and Graph delete labels are unchanged`() {
        assertFalse(isPermanentDeleteFolder(Protocols.IMAP, trashFolderId = 10L, spamFolderId = 11L, folderId = 10L))
        assertFalse(isPermanentDeleteFolder(Protocols.GRAPH, trashFolderId = 10L, spamFolderId = 11L, folderId = 10L))
        assertFalse(isZohoMailApiProtocol(Protocols.GRAPH_LEGACY))
    }

    @Test
    fun `incoming-only setting disables sending only for Zoho API accounts`() {
        assertTrue(NativeApiAccountCapabilities.isZohoIncomingOnly(
            Protocols.ZOHO_MAIL_API,
            mapOf(ZohoMailServerSettings.EXTRA_INCOMING_ONLY to "true"),
        ))
        assertFalse(NativeApiAccountCapabilities.isZohoIncomingOnly(Protocols.ZOHO_MAIL_API, emptyMap()))
        assertFalse(NativeApiAccountCapabilities.isZohoIncomingOnly(
            Protocols.IMAP,
            mapOf(ZohoMailServerSettings.EXTRA_INCOMING_ONLY to "true"),
        ))
    }
}
