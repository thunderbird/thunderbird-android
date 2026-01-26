plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("thunderbird.quality.detekt.typed")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("thunderbird.quality.spotless")
}

android {
    configureSharedConfig(project)

    buildFeatures {
        buildConfig = false
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
        jvmTarget.set(ThunderbirdProjectConfig.Compiler.jvmTarget)
    }
    sourceSets.all {
        compilerOptions {
            freeCompilerArgs.add("-Xwhen-guards")
        }
    }
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.jvm.main)
    implementation(libs.bundles.shared.jvm.android)

    testImplementation(libs.bundles.shared.jvm.test)
}
