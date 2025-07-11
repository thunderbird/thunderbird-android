package net.thunderbird.core.common.net

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.Test

class PortTest {
    @Test
    fun `valid port number`() {
        val port = Port(993)

        assertThat(port.value).isEqualTo(993)
    }

    @Test
    fun `negative port number should throw`() {
        assertFailure {
            Port(-1)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Not a valid port number: -1")
    }

    @Test
    fun `port number exceeding valid range should throw`() {
        assertFailure {
            Port(65536)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Not a valid port number: 65536")
    }
}
