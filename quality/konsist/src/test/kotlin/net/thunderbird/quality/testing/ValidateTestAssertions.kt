package net.thunderbird.quality.testing

import kotlin.test.Test
import net.thunderbird.quality.util.assertNoForbiddenImports

class ValidateTestAssertions {

    @Test
    fun `no class should use Truth assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead (see ADR-0003).",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("com.google.common.truth") }
    }

    @Test
    fun `no class should use Hamcrest assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead (see ADR-0003).",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("org.hamcrest") }
    }

    @Test
    fun `no class should use AssertJ assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead (see ADR-0003).",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("org.assertj") }
    }

    @Test
    fun `no class should use JUnit assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead of org.junit.Assert and junit.framework (see ADR-0003).",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("org.junit.Assert") || it.startsWith("junit.framework") }
    }

    @Test
    fun `no class should use Kotlin test assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead of kotlin.test assertions (see ADR-0003). " +
                "Only ${allowedKotlinTestImports.joinToString()} may be imported from kotlin.test.",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("kotlin.test.") && it !in allowedKotlinTestImports }
    }

    private companion object {
        val allowedKotlinTestImports = setOf(
            "kotlin.test.Test",
            "kotlin.test.BeforeTest",
            "kotlin.test.AfterTest",
            "kotlin.test.Ignore",
        )

        val assertkReplacements = mapOf(
            "assertEquals" to "assertThat(actual).isEqualTo(expected)",
            "assertNotEquals" to "assertThat(actual).isNotEqualTo(expected)",
            "assertArrayEquals" to "assertThat(actual).isEqualTo(expected)",
            "assertTrue" to "assertThat(x).isTrue()",
            "assertFalse" to "assertThat(x).isFalse()",
            "assertNull" to "assertThat(x).isNull()",
            "assertNotNull" to "assertThat(x).isNotNull()",
            "assertSame" to "assertThat(actual).isSameInstanceAs(expected)",
            "assertIs" to "assertThat(x).isInstanceOf<T>()",
            "assertFailsWith" to "assertFailure { }.isInstanceOf<T>()",
            "assertThrows" to "assertFailure { }.isInstanceOf<T>()",
            "assertFails" to "assertFailure { }",
            "fail" to "assertk.fail(message)",
        )

        fun assertkReplacementFor(importName: String): String =
            assertkReplacements[importName.substringAfterLast('.')] ?: "the assertk equivalent"
    }
}
