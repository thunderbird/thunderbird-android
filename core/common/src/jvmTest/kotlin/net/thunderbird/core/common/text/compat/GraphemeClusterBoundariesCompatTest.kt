package net.thunderbird.core.common.text.compat

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.Test

/**
 * Tests [GraphemeClusterBoundariesCompat] with the clusters a pre-Unicode 9 [java.text.BreakIterator] produces,
 * e.g. on Android 7.x.
 */
class GraphemeClusterBoundariesCompatTest {

    @Test
    fun `should keep boundaries of plain text`() {
        assertThat(correct("A", "B", "C")).containsExactly(0, 1, 2, 3)
        assertThat(correct("é", "x")).containsExactly(0, 2, 3)
        assertThat(correct()).containsExactly(0)
    }

    @Test
    fun `should join ZWJ sequences`() {
        assertThat(correct("🐦‍", "🔥")).containsExactly(0, 5)
        assertThat(
            correct("👨‍", "👩‍", "👧‍", "👦"),
        ).containsExactly(0, 11)
    }

    @Test
    fun `should join ZWJ sequences when the joiner is a separate cluster`() {
        assertThat(correct("🐦", "‍", "🔥")).containsExactly(0, 5)
    }

    @Test
    fun `should join skin tone modifiers`() {
        assertThat(correct("👍", "🏽")).containsExactly(0, 4)
        assertThat(correct("👨", "🏽‍", "💻")).containsExactly(0, 7)
    }

    @Test
    fun `should join tag sequences`() {
        val scotland = codePoints(0x1F3F4, 0xE0067, 0xE0062, 0xE0073, 0xE0063, 0xE0074, 0xE007F)
        val clusters = codePointClusters(scotland) + "A"

        assertThat(correct(*clusters.toTypedArray())).containsExactly(0, scotland.length, scotland.length + 1)
    }

    @Test
    fun `should split runs of regional indicators into pairs`() {
        val germany = "🇩🇪"
        val france = "🇫🇷"

        assertThat(correct(germany + france)).containsExactly(0, 4, 8)
        assertThat(correct(germany + france + germany, "A")).containsExactly(0, 4, 8, 12, 13)
        assertThat(correct(germany)).containsExactly(0, 4)
    }

    @Test
    fun `should keep boundaries between emoji sequences and text`() {
        assertThat(correct("🐦‍", "🔥", "P", "h")).containsExactly(0, 5, 6, 7)
    }

    private fun correct(vararg clusters: String): List<Int> {
        val text = clusters.joinToString("")
        val boundaries = clusters.runningFold(0) { boundary, cluster -> boundary + cluster.length }
        return GraphemeClusterBoundariesCompat.correct(text, boundaries)
    }

    private fun codePoints(vararg codePoints: Int): String = buildString {
        codePoints.forEach { appendCodePoint(it) }
    }

    private fun codePointClusters(text: String): List<String> =
        text.codePoints().toArray().map { codePoints(it) }
}
