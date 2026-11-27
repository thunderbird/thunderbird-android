package net.thunderbird.quality.testing

import com.lemonappdev.konsist.api.Konsist
import java.nio.file.Files
import kotlin.test.Test
import net.thunderbird.quality.util.QualityViolation

class MockitoUsageRuleTest {

    @Test
    fun `allows Mockito imports in DI test files`() {
        val testSubject = mapOf(
            "src/test/kotlin/FeatureDiModuleTest.kt" to """
                import org.mockito.Mockito
                import org.mockito.kotlin.mock as mockDependency
                import org.mockito.kotlin.*
            """.trimIndent(),
        )

        val violations = violations(testSubject)

        expectEqual(emptyList<QualityViolation>(), violations)
    }

    @Test
    fun `rejects direct aliased and wildcard imports outside DI tests`() {
        val testSubject = mapOf(
            "src/test/kotlin/FeatureTest.kt" to """
                import org.mockito.Mockito
                import org.mockito.kotlin.mock as mockDependency
                import org.mockito.kotlin.*
            """.trimIndent(),
        )

        val violations = violations(testSubject)

        expectEqual(3, violations.size)
        expectEqual(true, violations.all { "Mockito import outside a DI test" in it.reason })
        expectEqual(true, violations.all { "FeatureTest.kt:" in it.location })
    }

    @Test
    fun `allows files without Mockito imports`() {
        val testSubject = mapOf(
            "src/test/kotlin/FeatureTest.kt" to """
                import kotlin.test.Test
                import org.mockitology.Helper
            """.trimIndent(),
        )

        val violations = violations(testSubject)

        expectEqual(emptyList<QualityViolation>(), violations)
    }

    @Test
    fun `allows existing Mockito usage by exact relative path`() {
        val path = "feature/example/src/test/kotlin/FeatureTest.kt"
        val testSubject = mapOf(path to "import org.mockito.kotlin.mock")

        val violations = violations(testSubject, allowlist = setOf(path))

        expectEqual(emptyList<QualityViolation>(), violations)
    }

    @Test
    fun `exception does not allow the same filename in another module`() {
        val allowedPath = "feature/first/src/test/kotlin/FeatureTest.kt"
        val testSubject = mapOf(
            allowedPath to "import org.mockito.kotlin.mock",
            "feature/second/src/test/kotlin/FeatureTest.kt" to "import org.mockito.kotlin.mock",
        )

        val violations = violations(testSubject, allowlist = setOf(allowedPath))

        expectEqual(1, violations.size)
        expectEqual(true, "feature/second/" in violations.single().location)
    }

    @Test
    fun `rejects stale exceptions after Mockito imports are removed`() {
        val path = "src/test/kotlin/FeatureTest.kt"
        val testSubject = mapOf(path to "import kotlin.test.Test")

        val violations = violations(testSubject, allowlist = setOf(path))

        expectEqual(1, violations.size)
        expectEqual("Stale Mockito allowlist entry: $path.", violations.single().reason)
    }

    @Test
    fun `rejects stale exceptions for deleted files`() {
        val path = "src/test/kotlin/DeletedTest.kt"
        val testSubject = mapOf("src/test/kotlin/FeatureTest.kt" to "class FeatureTest")

        val violations = violations(testSubject, allowlist = setOf(path))

        expectEqual(1, violations.size)
        expectEqual("Stale Mockito allowlist entry: $path.", violations.single().reason)
    }

    @Test
    fun `DI tests do not need allowlist entries`() {
        val path = "src/test/kotlin/FeatureDiModuleTest.kt"
        val testSubject = mapOf(path to "import org.mockito.kotlin.mock")

        val violations = violations(testSubject, allowlist = setOf(path))

        expectEqual(1, violations.size)
        expectEqual("Stale Mockito allowlist entry: $path.", violations.single().reason)
    }

    @Test
    fun `rejects Mockito imports in non-DI helpers too`() {
        val testSubject = mapOf("src/main/kotlin/Helper.kt" to "import org.mockito.kotlin.mock")

        val violations = violations(testSubject)

        expectEqual(1, violations.size)
        expectEqual(true, "Helper.kt:" in violations.single().location)
    }

    private fun violations(
        sources: Map<String, String>,
        allowlist: Set<String> = emptySet(),
    ): List<QualityViolation> {
        val directory = Files.createTempDirectory("mockito-usage-rule").toFile()
        try {
            sources.forEach { (path, source) ->
                directory.resolve(path).apply {
                    parentFile.mkdirs()
                    writeText("package example\n\n$source")
                }
            }
            val files = Konsist.scopeFromExternalDirectory(directory.absolutePath).files
            return mockitoUsageViolations(files, directory.absolutePath, allowlist)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun expectEqual(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected: $expected\nActual: $actual")
    }
}
