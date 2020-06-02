package com.fsck.k9.crypto

import com.fsck.k9.Identity
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenPgpApiHelperTest {

    @Test
    fun buildUserId_withName_shouldCreateOpenPgpAccountName() {
        val identity = Identity(
                email = "user@domain.com",
                name = "Name"
        )

        val result = OpenPgpApiHelper.buildUserId(identity)

        assertEquals("Name <user@domain.com>", result)
    }

    @Test
    fun buildUserId_withoutName_shouldCreateOpenPgpAccountName() {
        val identity = Identity(
                email = "user@domain.com"
        )

        val result = OpenPgpApiHelper.buildUserId(identity)

        assertEquals("<user@domain.com>", result)
    }
}
