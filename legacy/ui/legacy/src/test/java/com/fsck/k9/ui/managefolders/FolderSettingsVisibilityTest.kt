package com.fsck.k9.ui.managefolders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderSettingsVisibilityTest {
    @Test
    fun `push is hidden for Graph backend`() {
        assertFalse(shouldShowPushPreference(isLocalOnly = false, isPushCapable = false))
    }

    @Test
    fun `push remains visible for push-capable providers`() {
        assertTrue(shouldShowPushPreference(isLocalOnly = false, isPushCapable = true))
    }

    @Test
    fun `push remains hidden for local-only folders`() {
        assertFalse(shouldShowPushPreference(isLocalOnly = true, isPushCapable = true))
    }
}
