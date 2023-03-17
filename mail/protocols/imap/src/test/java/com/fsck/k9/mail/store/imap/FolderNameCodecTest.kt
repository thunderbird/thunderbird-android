package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class FolderNameCodecTest {
    private var folderNameCode = FolderNameCodec()

    @Test
    fun `encode() with ASCII argument should return input`() {
        assertThat(folderNameCode.encode("ASCII")).isEqualTo("ASCII")
    }

    @Test
    fun `encode() with non-ASCII argument should return encoded string`() {
        assertThat(folderNameCode.encode("über")).isEqualTo("&APw-ber")
    }

    @Test
    fun `decode() with encoded argument should return decoded string`() {
        assertThat(folderNameCode.decode("&ANw-bergr&APYA3w-entr&AOQ-ger")).isEqualTo("Übergrößenträger")
    }

    @Test(expected = CharacterCodingException::class)
    fun `decode() with invalid encoded argument should throw`() {
        folderNameCode.decode("&12-foo")
    }
}
