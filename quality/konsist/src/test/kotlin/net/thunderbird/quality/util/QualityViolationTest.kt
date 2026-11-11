package net.thunderbird.quality.util

import kotlin.test.Test

class QualityViolationTest {
    @Test
    fun `renders a full file link and optional fix`() {
        val testSubject = QualityViolation("/project/Module.kt:12:3", "Wrong name", "Rename module")

        val result = testSubject.render()

        expectEqual("file:///project/Module.kt:12:3\n  Wrong name\n  Fix: Rename module", result)
    }

    @Test
    fun `omits absent suggestions and does not duplicate file scheme`() {
        val testSubject = QualityViolation("file:///project/Module.kt:12:3", "Wrong name")

        val result = testSubject.render()

        expectEqual("file:///project/Module.kt:12:3\n  Wrong name", result)
    }

    @Test
    fun `sorts findings and renders count`() {
        val testSubject = listOf(
            QualityViolation("/project/B.kt:2:1", "Second"),
            QualityViolation("/project/A.kt:1:1", "First"),
        )

        val result = renderQualityReport("Naming", testSubject)

        expectEqual(
            "Naming: 2 violations\n\nfile:///project/A.kt:1:1\n  First\n\n" +
                "file:///project/B.kt:2:1\n  Second",
            result,
        )
    }

    @Test
    fun `preserves sorted groups`() {
        val testSubject = listOf(
            QualityViolation("/project/B.kt:2:1", "Second", group = "b.import"),
            QualityViolation("/project/A.kt:1:1", "First", group = "a.import"),
        )

        val result = renderQualityReport("Imports", testSubject)

        expectEqual(
            "Imports: 2 violations\n\na.import\n\nfile:///project/A.kt:1:1\n  First\n\n" +
                "b.import\n\nfile:///project/B.kt:2:1\n  Second",
            result,
        )
    }

    @Test
    fun `empty findings pass and render zero count`() {
        val testSubject = emptyList<QualityViolation>()

        assertNoQualityViolations("Naming", testSubject)

        expectEqual("Naming: 0 violations", renderQualityReport("Naming", testSubject))
    }

    @Test
    fun `failure contains the rendered report`() {
        val testSubject = listOf(QualityViolation("/project/A.kt:1:1", "Wrong name"))

        val failure = try {
            assertNoQualityViolations("Naming", testSubject)
            null
        } catch (error: AssertionError) {
            error
        }

        expectEqual(renderQualityReport("Naming", testSubject), failure?.message)
        expectEqual("Naming: 1 violation\n\nfile:///project/A.kt:1:1\n  Wrong name", failure?.message)
    }

    // Keep renderer tests independent of assertion-library dependencies, like the reporting helper itself.
    private fun expectEqual(expected: String, actual: String?) {
        if (expected != actual) throw AssertionError("Expected:\n$expected\nActual:\n$actual")
    }
}
