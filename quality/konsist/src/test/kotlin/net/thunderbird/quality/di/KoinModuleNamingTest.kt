package net.thunderbird.quality.di

import kotlin.test.Test

class KoinModuleNamingTest {
    @Test
    fun `reports both incorrect property and filename`() {
        val testSubject = koinModuleNamingViolation("viewModule", "KoinModule.kt", LOCATION)

        expectEqual("Module property and filename do not follow the naming rule.", testSubject?.reason)
        expectEqual(
            "Rename property 'viewModule' to 'viewDiModule'. Rename file 'KoinModule.kt' to 'ViewDiModule.kt'.",
            testSubject?.suggestion,
        )
        expectEqual(LOCATION, testSubject?.location)
    }

    @Test
    fun `reports only the incorrect property`() {
        val testSubject = koinModuleNamingViolation("viewModule", "ViewDiModule.kt", LOCATION)

        expectEqual("Module property does not follow the naming rule.", testSubject?.reason)
        expectEqual("Rename property 'viewModule' to 'viewDiModule'.", testSubject?.suggestion)
    }

    @Test
    fun `reports only the incorrect filename`() {
        val testSubject = koinModuleNamingViolation("viewDiModule", "KoinModule.kt", LOCATION)

        expectEqual("Filename does not match the DI module name.", testSubject?.reason)
        expectEqual("Rename file 'KoinModule.kt' to 'ViewDiModule.kt'.", testSubject?.suggestion)
    }

    @Test
    fun `correct property and filename pass`() {
        val testSubject = koinModuleNamingViolation("viewDiModule", "ViewDiModule.kt", LOCATION)

        expectEqual(null, testSubject)
    }

    @Test
    fun `preserves platform suffix when suggesting filename`() {
        val testSubject = koinModuleNamingViolation("platformDiModule", "KoinModule.android.kt", LOCATION)

        expectEqual("Rename file 'KoinModule.android.kt' to 'PlatformDiModule.android.kt'.", testSubject?.suggestion)
    }

    @Test
    fun `correct platform filename passes`() {
        val testSubject = koinModuleNamingViolation("platformDiModule", "PlatformDiModule.jvm.kt", LOCATION)

        expectEqual(null, testSubject)
    }

    private fun expectEqual(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected: $expected\nActual: $actual")
    }

    private companion object {
        const val LOCATION = "/project/src/main/kotlin/example/KoinModule.kt:14:1"
    }
}
