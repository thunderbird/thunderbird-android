package net.thunderbird.quality.di

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import java.nio.file.Files
import kotlin.test.Test

class KoinGraphTestNamingRuleTest {
    @Test
    fun `module names come from verify calls rather than the test class name`() {
        val testSubject = "featureMailMessageListDiModule.verify(extraTypes = listOf(Context::class))"

        val names = koinGraphTestModuleNames(testSubject)

        expectEqual(setOf("featureMailMessageListDiModule"), names)
    }

    @Test
    fun `module names come from modules calls and ignore external dependency fixtures`() {
        val testSubject = "startKoin { modules(featureChangelogDiModule, externalDependencies) }"

        val names = koinGraphTestModuleNames(testSubject)

        expectEqual(setOf("featureChangelogDiModule"), names)
    }

    @Test
    fun `multiple module references are returned without choosing one arbitrarily`() {
        val testSubject = "modules(firstDiModule, secondDiModule)"

        val names = koinGraphTestModuleNames(testSubject)

        expectEqual(setOf("firstDiModule", "secondDiModule"), names)
    }

    @Test
    fun `a module name without a graph call is not sufficient`() {
        val testSubject = "import example.featureDiModule\n// featureDiModule is provided externally"

        val names = koinGraphTestModuleNames(testSubject)

        expectEqual(emptySet<String>(), names)
    }

    @Test
    fun `TestApp fixtures are not graph test candidates`() {
        withGraphTest(className = "TestApp", filePath = "example/feature/TestApp.kt") { testSubject, _ ->
            expectEqual(false, isKoinGraphTestCandidate(testSubject))
        }
    }

    @Test
    fun `ordinary graph tests remain candidates`() {
        withGraphTest { testSubject, _ ->
            expectEqual(true, isKoinGraphTestCandidate(testSubject))
        }
    }

    @Test
    fun `matching class file package and source directory pass`() {
        withGraphTest { testSubject, module ->
            val violation = koinGraphTestNamingViolation(testSubject, module)

            expectEqual(null, violation)
        }
    }

    @Test
    fun `legacy class and filename suggestions use the actual tested module`() {
        withGraphTest(
            className = "FeatureMessageListModuleKtTest",
            filePath = "example/feature/FeatureMessageListModuleKtTest.kt",
            moduleName = "featureMailMessageListDiModule",
        ) { testSubject, module ->
            val violation = koinGraphTestNamingViolation(testSubject, module)

            expectEqual(
                "Class: FeatureMessageListModuleKtTest -> FeatureMailMessageListDiModuleTest; " +
                    "File: FeatureMessageListModuleKtTest.kt -> FeatureMailMessageListDiModuleTest.kt",
                violation?.suggestion,
            )
        }
    }

    @Test
    fun `incorrect class name does not request changes to a correct file`() {
        withGraphTest(className = "OldTest") { testSubject, module ->
            val violation = koinGraphTestNamingViolation(testSubject, module)

            expectEqual("Class: OldTest -> FeatureDiModuleTest", violation?.suggestion)
        }
    }

    @Test
    fun `incorrect filename is reported even when the class name is correct`() {
        withGraphTest(filePath = "example/feature/OldTest.kt") { testSubject, module ->
            val violation = koinGraphTestNamingViolation(testSubject, module)

            expectEqual("File: OldTest.kt -> FeatureDiModuleTest.kt", violation?.suggestion)
        }
    }

    @Test
    fun `test package must match the tested module package`() {
        withGraphTest(packageName = "example.other") { testSubject, module ->
            val violation = koinGraphTestNamingViolation(testSubject, module)

            expectEqual("Package: example.other -> example.feature", violation?.suggestion)
        }
    }

    @Test
    fun `test directory must mirror the tested module even if its package is correct`() {
        withGraphTest(filePath = "example/feature/impl/FeatureDiModuleTest.kt") { testSubject, module ->
            val violation = koinGraphTestNamingViolation(testSubject, module)

            expectEqual(
                "Path: example/feature/impl/FeatureDiModuleTest.kt -> example/feature/FeatureDiModuleTest.kt",
                violation?.suggestion,
            )
        }
    }

    @Test
    fun `platform filename suffix is retained`() {
        withGraphTest(className = "OldTest", filePath = "example/feature/OldTest.android.kt") { testSubject, module ->
            val violation = koinGraphTestNamingViolation(testSubject, module)

            expectEqual(
                "Class: OldTest -> FeatureDiModuleTest; File: OldTest.android.kt -> FeatureDiModuleTest.android.kt",
                violation?.suggestion,
            )
        }
    }

    private fun withGraphTest(
        className: String = "FeatureDiModuleTest",
        filePath: String = "example/feature/FeatureDiModuleTest.kt",
        packageName: String = "example.feature",
        moduleName: String = "featureDiModule",
        block: (KoClassDeclaration, KoPropertyDeclaration) -> Unit,
    ) {
        val directory = Files.createTempDirectory("koin-graph-test-naming").toFile()
        try {
            val moduleFile = directory.resolve("src/commonMain/kotlin/example/feature/FeatureDiModule.kt")
            moduleFile.parentFile.mkdirs()
            moduleFile.writeText("package example.feature\nimport org.koin.dsl.module\nval $moduleName = module { }")
            val testFile = directory.resolve("src/commonTest/kotlin/$filePath")
            testFile.parentFile.mkdirs()
            testFile.writeText(
                """
                package $packageName
                import org.koin.test.verify.verify
                class $className {
                    fun verifyGraph() { $moduleName.verify() }
                }
                """.trimIndent(),
            )
            val files = Konsist.scopeFromExternalDirectory(directory.absolutePath).files
            val module = files.single { it.path == moduleFile.absolutePath }.properties().single()
            val testClass = files.single { it.path == testFile.absolutePath }.classes().single()
            block(testClass, module)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun expectEqual(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected: $expected\nActual: $actual")
    }
}
