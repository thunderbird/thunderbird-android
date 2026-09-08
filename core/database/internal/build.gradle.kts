plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.database.internal"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.database.api)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidHostTest.dependencies {
            implementation(libs.androidx.sqlite.framework)
            implementation(libs.androidx.test.core)
            implementation(libs.robolectric)
        }
        jvmTest.dependencies {
            implementation(libs.androidx.room3.testing)
        }
    }
}

dependencies {
    add("kspAndroidHostTest", libs.androidx.room3.compiler)
    add("kspJvmTest", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
