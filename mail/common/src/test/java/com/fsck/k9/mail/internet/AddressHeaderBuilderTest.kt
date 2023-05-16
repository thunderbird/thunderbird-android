package com.fsck.k9.mail.internet

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.crlf
import org.junit.Test

class AddressHeaderBuilderTest {

    @Test
    fun createHeaderValue_withSingleAddress() {
        val addresses = arrayOf(Address("test@domain.example"))

        val headerValue = AddressHeaderBuilder.createHeaderValue(addresses)

        assertThat(headerValue).isEqualTo("test@domain.example")
    }

    @Test
    fun createHeaderValue_withTwoAddressesThatFitOnSingleLine() {
        val addresses = arrayOf(
            Address("one@domain.example"),
            Address("two@domain.example"),
        )

        val headerValue = AddressHeaderBuilder.createHeaderValue(addresses)

        assertThat(headerValue).isEqualTo("one@domain.example, two@domain.example")
    }

    @Test
    fun createHeaderValue_withMultipleAddressesThatNeedWrapping() {
        val addresses = arrayOf(
            Address("one@domain.example", "Person One"),
            Address("two+because.i.can@this.is.quite.some.domain.example", "Person \"Long Email Address\" Two"),
            Address("three@domain.example", "Person Three"),
            Address("four@domain.example", "Person Four"),
            Address("five@domain.example", "Person Five"),
        )

        val headerValue = AddressHeaderBuilder.createHeaderValue(addresses)

        assertThat(headerValue).isEqualTo(
            """
            |Person One <one@domain.example>,
            | "Person \"Long Email Address\" Two" <two+because.i.can@this.is.quite.some.domain.example>,
            | Person Three <three@domain.example>, Person Four <four@domain.example>,
            | Person Five <five@domain.example>
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun createHeaderValue_withoutAddresses_shouldThrow() {
        assertFailure {
            AddressHeaderBuilder.createHeaderValue(emptyArray())
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("addresses must not be empty")
    }
}
