package net.thunderbird.quality.di

import com.lemonappdev.konsist.api.Konsist
import java.nio.file.Files
import kotlin.test.Test

class KoinModuleVisibilityTest {
    @Test
    fun `implicit and explicit public visibility are detected`() {
        val directory = Files.createTempDirectory("koin-visibility").toFile()
        try {
            directory.resolve("Fixture.kt").writeText(
                """
                package example

                val implicitDiModule = module { }
                public val explicitDiModule = module { }
                internal val helperDiModule = module { }
                private val privateDiModule = module { }
                """.trimIndent(),
            )
            val testSubject = Konsist.scopeFromExternalDirectory(directory.absolutePath)
                .files.single().properties()

            val declarations = testSubject.map { it.toKoinModuleVisibility() }

            expectEqual(
                setOf("example.implicitDiModule", "example.explicitDiModule"),
                declarations.filter { it.isPublic }.map { it.qualifiedName }.toSet(),
            )
            expectEqual(listOf("example.helperDiModule"), declarations.filter { it.isInternal }.map { it.qualifiedName })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `one public entry point with internal helpers passes`() {
        val testSubject = listOf(declaration("entry"), declaration("helper", isPublic = false, isInternal = true))

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(emptyList<String>(), violations.map { it.reason })
    }

    @Test
    fun `multiple public entry points report each declaration`() {
        val testSubject = listOf(declaration("entry"), declaration("helper"))

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(testSubject.map { it.location }, violations.map { it.location })
        expectEqual(true, violations.all { "2 DI entry points: example.entry, example.helper" in it.reason })
    }

    @Test
    fun `module without public entry point fails`() {
        val testSubject = listOf(declaration("helper", isPublic = false, isInternal = true))

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(1, violations.size)
        expectEqual(true, "no public DI entry point" in violations.single().reason)
    }

    @Test
    fun `private supporting modules must be internal`() {
        val testSubject = listOf(declaration("entry"), declaration("helper", isPublic = false))

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(1, violations.size)
        expectEqual(testSubject.last().location, violations.single().location)
        expectEqual("Mark the supporting DI module internal.", violations.single().suggestion)
    }

    @Test
    fun `same entry point in alternative source sets counts once`() {
        val testSubject = listOf(
            declaration("entry", sourceSet = "debug"),
            declaration("entry", sourceSet = "release"),
            declaration("helper", isPublic = false, isInternal = true),
        )

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(0, violations.size)
    }

    @Test
    fun `entry point visibility must agree across source sets`() {
        val testSubject = listOf(
            declaration("entry", sourceSet = "androidMain"),
            declaration("entry", sourceSet = "jvmMain", isPublic = false, isInternal = true),
        )

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(1, violations.size)
        expectEqual(testSubject.last().location, violations.single().location)
        expectEqual(true, "inconsistent visibility" in violations.single().reason)
    }

    @Test
    fun `gradle modules with identical leaf names are checked independently`() {
        val testSubject = listOf(
            declaration("entry", gradleModule = "feature/account/internal"),
            declaration("entry", gradleModule = "feature/message/internal"),
        )

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(0, violations.size)
    }

    @Test
    fun `same property name in different packages is not one entry point`() {
        val testSubject = listOf(
            declaration("entry"),
            declaration("entry").copy(qualifiedName = "other.entry"),
        )

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(2, violations.size)
    }

    @Test
    fun `modules without di declarations need no entry point`() {
        val testSubject = emptyList<KoinModuleVisibility>()

        val violations = koinModuleVisibilityViolations(testSubject)

        expectEqual(0, violations.size)
    }

    private fun declaration(
        name: String,
        isPublic: Boolean = true,
        isInternal: Boolean = false,
        gradleModule: String = "feature/example/internal",
        sourceSet: String = "main",
    ) = KoinModuleVisibility(
        gradleModule = gradleModule,
        qualifiedName = "example.$name",
        location = "/project/$gradleModule/src/$sourceSet/kotlin/example/$name.kt:3:1",
        isPublic = isPublic,
        isInternal = isInternal,
    )

    // Use the same dependency-free failure mechanism as the quality reporting tests.
    private fun expectEqual(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected: $expected\nActual: $actual")
    }
}
