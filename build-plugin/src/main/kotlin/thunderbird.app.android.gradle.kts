plugins {
    id("com.android.application")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("net.thunderbird.gradle.plugin.quality.detekt")
    id("net.thunderbird.gradle.plugin.quality.spotless")
}

android {
    compileSdk = ThunderbirdProjectConfig.Android.sdkCompile

    defaultConfig {
        minSdk = ThunderbirdProjectConfig.Android.sdkMin
        targetSdk = ThunderbirdProjectConfig.Android.sdkTarget

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
        targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = true
        @Suppress("UnstableApiUsage")
        lintConfig = isolated.rootProject.projectDirectory.file("config/lint/lint.xml").asFile
        checkReleaseBuilds = System.getenv("CI_CHECK_RELEASE_BUILDS")?.toBoolean() ?: true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/README",
                "/META-INF/README.md",
                "/META-INF/CHANGES",
                "/LICENSE.txt",
            )
        }
    }

    testOptions {
        unitTests.all {
            it.jvmArgs(ThunderbirdProjectConfig.Testing.robolectricJvmArgs)
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = ThunderbirdProjectConfig.Compiler.jvmTarget
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar.nio)

    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.android.app)

    testImplementation(libs.bundles.shared.android.app.test)
}

tasks.register("testsOnCi") {
    dependsOn(
        tasks.withType<Test>().matching {
            it.name != "testReleaseUnitTest"
        }
    )
}
