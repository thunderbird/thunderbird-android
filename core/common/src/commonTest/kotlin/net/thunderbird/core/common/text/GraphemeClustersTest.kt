package net.thunderbird.core.common.text

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class GraphemeClustersTest {

    @Test
    fun `count should count ASCII characters`() {
        assertThat(GraphemeClusters.count("")).isEqualTo(0)
        assertThat(GraphemeClusters.count("ABC")).isEqualTo(3)
    }

    @Test
    fun `count should count multi-code-point characters as one`() {
        assertThat(GraphemeClusters.count(PHOENIX)).isEqualTo(1)
        assertThat(GraphemeClusters.count(FAMILY)).isEqualTo(1)
        assertThat(GraphemeClusters.count(THUMBS_UP_MEDIUM_SKIN_TONE)).isEqualTo(1)
        assertThat(GraphemeClusters.count(FLAG_GERMANY)).isEqualTo(1)
        assertThat(GraphemeClusters.count(E_WITH_COMBINING_ACUTE)).isEqualTo(1)
    }

    @Test
    fun `count should count adjacent emoji separately`() {
        assertThat(GraphemeClusters.count("🔥🔥🔥🔥")).isEqualTo(4)
        assertThat(GraphemeClusters.count("$FLAG_GERMANY$FLAG_FRANCE")).isEqualTo(2)
        assertThat(GraphemeClusters.count("$PHOENIX$FAMILY")).isEqualTo(2)
    }

    @Test
    fun `take should not split multi-code-point characters`() {
        assertThat(GraphemeClusters.take("${PHOENIX}Phoenix", 2)).isEqualTo("${PHOENIX}P")
        assertThat(GraphemeClusters.take("$FAMILY$PHOENIX$FLAG_GERMANY", 2)).isEqualTo("$FAMILY$PHOENIX")
        assertThat(GraphemeClusters.take("$FLAG_GERMANY$FLAG_FRANCE", 1)).isEqualTo(FLAG_GERMANY)
    }

    @Test
    fun `take should return whole text when it has fewer clusters`() {
        assertThat(GraphemeClusters.take("A", 2)).isEqualTo("A")
        assertThat(GraphemeClusters.take(PHOENIX, 2)).isEqualTo(PHOENIX)
        assertThat(GraphemeClusters.take("", 2)).isEqualTo("")
    }

    @Test
    fun `take should return empty string for zero or negative count`() {
        assertThat(GraphemeClusters.take("ABC", 0)).isEqualTo("")
        assertThat(GraphemeClusters.take("ABC", -1)).isEqualTo("")
    }

    private companion object {
        const val PHOENIX = "🐦‍🔥"
        const val FAMILY = "👨‍👩‍👧‍👦"
        const val THUMBS_UP_MEDIUM_SKIN_TONE = "👍🏽"
        const val FLAG_GERMANY = "🇩🇪"
        const val FLAG_FRANCE = "🇫🇷"
        const val E_WITH_COMBINING_ACUTE = "é"
    }
}
