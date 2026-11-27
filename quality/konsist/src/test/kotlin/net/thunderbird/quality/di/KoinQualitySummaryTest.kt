package net.thunderbird.quality.di

import kotlin.test.Test

class KoinQualitySummaryTest {
    @Test
    fun `shows aligned counts including zero and singular`() {
        val testSubject = listOf(
            KoinRuleCount("Naming", count = 75),
            KoinRuleCount("Visibility", count = 0),
            KoinRuleCount("One per file", count = 1),
        )

        val result = renderKoinQualitySummary(testSubject)

        expectEqual(
            """
            Koin quality checks
              Naming:       75 violations
              Visibility:   0 violations
              One per file: 1 violation
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `distinguishes ignored and filtered out rules`() {
        val testSubject = listOf(
            KoinRuleCount("Naming"),
            KoinRuleCount("Compiler DSL", skipped = true),
        )

        val result = renderKoinQualitySummary(testSubject)

        expectEqual(
            """
            Koin quality checks
              Naming:       not run
              Compiler DSL: skipped
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `shows counts when an ignored rule was explicitly evaluated`() {
        val testSubject = listOf(KoinRuleCount("Naming", count = 0, skipped = true))

        val result = renderKoinQualitySummary(testSubject)

        expectEqual("Koin quality checks\n  Naming: 0 violations", result)
    }

    @Test
    fun `empty summary has no dangling newline`() {
        val testSubject = emptyList<KoinRuleCount>()

        val result = renderKoinQualitySummary(testSubject)

        expectEqual("Koin quality checks", result)
    }

    private fun expectEqual(expected: String, actual: String) {
        if (expected != actual) throw AssertionError("Expected:\n$expected\nActual:\n$actual")
    }
}
