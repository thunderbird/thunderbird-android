package net.thunderbird.quality.di

import com.lemonappdev.konsist.api.Konsist
import java.nio.file.Files
import kotlin.test.Test

class KoinModuleDeclarationsRuleTest {
    @Test
    fun `counts two direct module declarations`() {
        val source = """
            import org.koin.dsl.module
            val firstDiModule = module { }
            val secondDiModule = module { }
        """.trimIndent()

        val names = declarationNames(source)

        expectEqual(listOf("firstDiModule", "secondDiModule"), names)
    }

    @Test
    fun `counts expect and initialized declarations together`() {
        val source = """
            import org.koin.core.module.Module
            import org.koin.dsl.module
            expect val platformDiModule: Module
            val featureDiModule = module { }
        """.trimIndent()

        val names = declarationNames(source)

        expectEqual(listOf("platformDiModule", "featureDiModule"), names)
    }

    @Test
    fun `counts fully qualified module initializers`() {
        val source = """
            val firstDiModule = org.koin.dsl.module { }
            val secondDiModule = org.koin.dsl.module { }
        """.trimIndent()

        val names = declarationNames(source)

        expectEqual(listOf("firstDiModule", "secondDiModule"), names)
    }

    @Test
    fun `counts aliased module initializers`() {
        val source = """
            import org.koin.dsl.module as koinModule
            val firstDiModule = koinModule { }
            val secondDiModule = koinModule { }
        """.trimIndent()

        val names = declarationNames(source)

        expectEqual(listOf("firstDiModule", "secondDiModule"), names)
    }

    @Test
    fun `counts declarations initialized by a helper`() {
        val source = """
            import org.koin.core.module.Module
            val firstDiModule: Module = buildModule()
            val secondDiModule: Module = buildModule()
        """.trimIndent()

        val names = declarationNames(source)

        expectEqual(listOf("firstDiModule", "secondDiModule"), names)
    }

    @Test
    fun `ignores nested module properties`() {
        val source = """
            import org.koin.dsl.module
            val featureDiModule = module { }
            object Helper {
                val nestedDiModule = module { }
            }
        """.trimIndent()

        val names = declarationNames(source)

        expectEqual(listOf("featureDiModule"), names)
    }

    @Test
    fun `ignores unrelated top level properties`() {
        val source = """
            val featureDiModule = org.koin.dsl.module { }
            val configuration = "featureDiModule"
            val featureDiModuleName = "feature"
        """.trimIndent()

        val names = declarationNames(source)

        expectEqual(listOf("featureDiModule"), names)
    }

    @Test
    fun `returns no declarations for a file without DI modules`() {
        val source = "val configuration = true"

        val names = declarationNames(source)

        expectEqual(emptyList<String>(), names)
    }

    private fun declarationNames(source: String): List<String> {
        val directory = Files.createTempDirectory("koin-module-declarations").toFile()
        try {
            directory.resolve("Fixture.kt").writeText("package example\n\n$source")
            val testSubject = Konsist.scopeFromExternalDirectory(directory.absolutePath).files.single()
            return koinModuleDeclarations(testSubject).map { it.name }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun expectEqual(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected: $expected\nActual: $actual")
    }
}
