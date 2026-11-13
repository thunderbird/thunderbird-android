package net.thunderbird.quality.di

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import java.nio.file.Files
import kotlin.test.Test

class KoinModuleVisibilityRuleTest {
    @Test
    fun `root package permits normal kotlin visibility without explicit public`() {
        withProperties("example") { testSubject ->
            val violations = testSubject.mapNotNull { koinModuleVisibilityViolation(it, "example") }

            expectEqual(0, violations.size)
        }
    }

    @Test
    fun `subpackage di modules must explicitly be internal`() {
        withProperties("example.support") { testSubject ->
            val violations = testSubject.mapNotNull { koinModuleVisibilityViolation(it, "example") }

            expectEqual(4, violations.size)
            expectEqual(true, violations.all { "outside root package 'example' and must be internal" in it.reason })
            expectEqual(
                testSubject.filterNot { it.hasInternalModifier }.map { it.location }.toSet(),
                violations.map { it.location }.toSet(),
            )
        }
    }

    @Test
    fun `root is the longest shared package prefix on component boundaries`() {
        val testSubject = listOf(
            listOf("example.feature", "example.feature.data", "example.feature.ui") to "example.feature",
            listOf("example.feature.data", "example.feature.ui") to "example.feature",
            listOf("example.foo", "example.foobar") to "example",
            listOf("example.feature", "example.feature") to "example.feature",
            listOf("example.feature.data") to "example.feature.data",
            listOf("example.feature", "other.feature") to null,
            emptyList<String>() to null,
        )

        testSubject.forEach { (packages, expected) ->
            val root = commonProductionPackage(packages)

            expectEqual(expected, root)
        }
    }

    @Test
    fun `packages across kmp source sets contribute to the same root`() {
        val directory = Files.createTempDirectory("koin-packages").toFile()
        try {
            mapOf(
                "commonMain" to "example.feature",
                "androidMain" to "example.feature.android",
                "jvmMain" to "example.feature.jvm",
            ).forEach { (sourceSet, packageName) ->
                directory.resolve("src/$sourceSet/kotlin/Fixture.kt").apply {
                    parentFile.mkdirs()
                    writeText("package $packageName\nclass Fixture")
                }
            }
            val testSubject = Konsist.scopeFromExternalDirectory(directory.absolutePath).files

            val root = commonProductionPackage(testSubject.mapNotNull { it.packagee?.name })

            expectEqual("example.feature", root)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `normal package layout needs no informational note`() {
        val testSubject = listOf("example.feature", "example.feature.data")

        val note = productionPackageLayoutNote(testSubject)

        expectEqual(null, note)
    }

    @Test
    fun `root with no files directly in it is reported for review`() {
        val testSubject = listOf("example.feature.data", "example.feature.ui")

        val note = productionPackageLayoutNote(testSubject)

        expectEqual(
            "Derived root 'example.feature' has no production files directly in it. Review the package layout.",
            note,
        )
    }

    @Test
    fun `unrelated package trees are reported and visibility is not guessed`() {
        val testSubject = listOf("example.feature", "other.feature")

        val root = commonProductionPackage(testSubject)
        val note = productionPackageLayoutNote(testSubject)

        expectEqual(null, root)
        expectEqual(
            "No shared production package prefix. Review the package layout.",
            note,
        )
        withProperties("example.feature") { declarations ->
            expectEqual(0, declarations.mapNotNull { koinModuleVisibilityViolation(it, root) }.size)
        }
    }

    private fun withProperties(packageName: String, block: (List<KoPropertyDeclaration>) -> Unit) {
        val directory = Files.createTempDirectory("koin-visibility").toFile()
        try {
            directory.resolve("Fixture.kt").writeText(
                """
                package $packageName

                val implicitDiModule = module { }
                public val explicitDiModule = module { }
                internal val helperDiModule = module { }
                private val privateDiModule = module { }
                expect val platformDiModule: org.koin.core.module.Module
                """.trimIndent(),
            )
            val properties = Konsist.scopeFromExternalDirectory(directory.absolutePath).files.single().properties()
            block(properties)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun expectEqual(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected: $expected\nActual: $actual")
    }
}
