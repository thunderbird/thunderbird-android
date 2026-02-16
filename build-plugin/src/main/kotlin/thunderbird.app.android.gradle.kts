plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("thunderbird.quality.detekt.typed")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("thunderbird.quality.spotless")
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
        lintConfig = project.file("${project.rootProject.projectDir}/config/lint/lint.xml")
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

    /**
     * Disables the C2 compiler for Robolectric tests.
     *
     * This is a workaround for a known issue where Robolectric tests can fail on JDK 17+
     * with a "failed to compile" error. The issue is related to the Tiered Compilation in the JVM,
     * specifically the C2 (server) compiler. Disabling C2 forces the JVM to use the C1 (client)
     * compiler, which avoids the problem.
     *
     * The official workaround uses `-XX:+TieredCompilation -XX:TieredStopAtLevel=1`, but just
     * `-XX:TieredStopAtLevel=3` seems to work. In case the flakiness still happens, we can
     * use the workaround mentioned in the issue.
     *
     * See: https://github.com/robolectric/robolectric/issues/3202
     */
    testOptions {
        unitTests.all {
            it.jvmArgs("-XX:TieredStopAtLevel=3")
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

    implementation(libs.bundles.shared.jvm.android.app)

    testImplementation(libs.bundles.shared.jvm.test)
}
