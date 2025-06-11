plugins {
    id(ThunderbirdPlugins.Library.kmp)
    id("kotlin-parcelize")
}

android {
    namespace = "net.thunderbird.core.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.implLegacy)
            implementation(projects.core.logging.api)
            implementation(projects.core.logging.implFile)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.annotation)
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xexpect-actual-classes",
            ),
        )
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-P",
            "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=net.thunderbird.core.common.io.KmpParcelize",
            "-Xexpect-actual-classes",
        )
    }
}
