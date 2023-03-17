package assertk.assertions

import assertk.assertThat
import kotlin.test.Test

class ListExtensionsKtTest {

    @Test
    fun `containsNoDuplicates() should succeed with no duplicates`() {
        val list = listOf("a", "b", "c")

        assertThat(list).containsNoDuplicates()
    }

    @Test
    fun `containsNoDuplicates() should fail with duplicates`() {
        val list = listOf("a", "b", "c", "a", "a")

        assertThat {
            assertThat(list).containsNoDuplicates()
        }.isFailure()
            .hasMessage(
                """
                expected to contain no duplicates but found: <["a", "a"]>
                """.trimIndent(),
            )
    }
}
