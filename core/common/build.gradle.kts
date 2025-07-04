plugins {
    id(ThunderbirdPlugins.Library.kmp)
    id("kotlin-parcelize")
}

android {
    namespace = "net.thunderbird.core.common"
}

kotlin {
    sourceSets {
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
