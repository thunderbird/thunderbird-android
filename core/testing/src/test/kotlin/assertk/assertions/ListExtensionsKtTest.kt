package assertk.assertions

import assertk.assertFailure
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

        assertFailure {
            assertThat(list).containsNoDuplicates()
        }.hasMessage("""expected to contain no duplicates but found: <["a", "a"]>""")
    }
}
