package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class FormatFlowedHelperTest {
    @Test
    fun `plain text and format=flowed`() {
        val contentTypeHeader = "text/plain; format=flowed"

        val result = FormatFlowedHelper.checkFormatFlowed(contentTypeHeader)

        assertThat(result.isFormatFlowed).isTrue()
        assertThat(result.isDelSp).isFalse()
    }

    @Test
    fun `plain text and format=flowed and delsp=yes`() {
        val contentTypeHeader = "text/plain; format=flowed; delsp=yes"

        val result = FormatFlowedHelper.checkFormatFlowed(contentTypeHeader)

        assertThat(result.isFormatFlowed).isTrue()
        assertThat(result.isDelSp).isTrue()
    }

    @Test
    fun `plain text without format=flowed`() {
        val contentTypeHeader = "text/plain"

        val result = FormatFlowedHelper.checkFormatFlowed(contentTypeHeader)

        assertThat(result.isFormatFlowed).isFalse()
    }

    @Test
    fun `plain text without format=flowed but delsp=yes`() {
        val contentTypeHeader = "text/plain; delsp=yes"

        val result = FormatFlowedHelper.checkFormatFlowed(contentTypeHeader)

        assertThat(result.isFormatFlowed).isFalse()
        assertThat(result.isDelSp).isFalse()
    }

    @Test
    fun `HTML and format=flowed`() {
        val contentTypeHeader = "text/html; format=flowed"

        val result = FormatFlowedHelper.checkFormatFlowed(contentTypeHeader)

        assertThat(result.isFormatFlowed).isFalse()
    }

    @Test
    fun `HTML and format=flowed and delsp=yes`() {
        val contentTypeHeader = "text/html; format=flowed; delsp=yes"

        val result = FormatFlowedHelper.checkFormatFlowed(contentTypeHeader)

        assertThat(result.isFormatFlowed).isFalse()
        assertThat(result.isDelSp).isFalse()
    }
}
