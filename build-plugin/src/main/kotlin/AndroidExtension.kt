import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

internal fun CommonExtension<*, *, *, *, *, *>.configureSharedConfig(project: Project) {
    compileSdk = ThunderbirdProjectConfig.Android.sdkCompile

    defaultConfig {
        compileSdk = ThunderbirdProjectConfig.Android.sdkCompile
        minSdk = ThunderbirdProjectConfig.Android.sdkMin

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
        targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
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
}

internal fun CommonExtension<*, *, *, *, *, *>.configureSharedComposeConfig(libs: LibrariesForLibs) {
    buildFeatures {
        compose = true
    }
}

internal fun DependencyHandler.configureSharedComposeDependencies(libs: LibrariesForLibs) {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.shared.jvm.android.compose)

    debugImplementation(libs.bundles.shared.jvm.android.compose.debug)

    testImplementation(libs.bundles.shared.jvm.test.compose)

    androidTestImplementation(libs.bundles.shared.jvm.androidtest.compose)
}
