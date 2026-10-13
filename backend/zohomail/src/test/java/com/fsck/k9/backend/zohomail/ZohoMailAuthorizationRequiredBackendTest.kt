package com.fsck.k9.backend.zohomail

import com.fsck.k9.mail.AuthenticationFailedException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class ZohoMailAuthorizationRequiredBackendTest {
    @Test
    fun `missing OAuth backend is not cached and does not advertise capabilities`() {
        val backend = ZohoMailAuthorizationRequiredBackend

        assertFalse(backend.isCacheable)
        assertFalse(backend.isPushCapable)
        assertFalse(backend.supportsMove)
        assertFalse(backend.supportsFlags)
    }

    @Test
    fun `backend reports a reauthorization error when an operation is attempted`() {
        val error = assertThrows(AuthenticationFailedException::class.java) {
            ZohoMailAuthorizationRequiredBackend.refreshFolderList()
        }

        assertTrueMessage(error.message)
    }

    private fun assertTrueMessage(value: String?) {
        org.junit.Assert.assertTrue(value.orEmpty().contains("authorization is required"))
    }
}
