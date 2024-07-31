package com.fsck.k9.crypto

import app.k9mail.legacy.account.Identity
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OpenPgpApiHelperTest {

    @Test
    fun buildUserId_withName_shouldCreateOpenPgpAccountName() {
        val identity = Identity(
            email = "user@domain.com",
            name = "Name",
        )

        val result = OpenPgpApiHelper.buildUserId(identity)

        assertThat(result).isEqualTo("Name <user@domain.com>")
    }

    @Test
    fun buildUserId_withoutName_shouldCreateOpenPgpAccountName() {
        val identity = Identity(
            email = "user@domain.com",
        )

        val result = OpenPgpApiHelper.buildUserId(identity)

        assertThat(result).isEqualTo("<user@domain.com>")
    }
}
