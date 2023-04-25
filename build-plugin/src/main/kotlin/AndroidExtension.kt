import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.artifacts.dsl.DependencyHandler

internal fun CommonExtension<*, *, *, *>.configureSharedConfig() {
    compileSdk = ThunderbirdProjectConfig.androidSdkCompile

    defaultConfig {
        compileSdk = ThunderbirdProjectConfig.androidSdkCompile
        minSdk = ThunderbirdProjectConfig.androidSdkMin

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = ThunderbirdProjectConfig.javaCompatibilityVersion
        targetCompatibility = ThunderbirdProjectConfig.javaCompatibilityVersion
    }

    lint {
        abortOnError = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

internal fun CommonExtension<*, *, *, *>.configureSharedComposeConfig(
    libs: LibrariesForLibs,
) {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

internal fun DependencyHandler.configureSharedComposeDependencies(
    libs: LibrariesForLibs,
) {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.shared.jvm.android.compose)

    debugImplementation(libs.bundles.shared.jvm.android.compose.debug)

    testImplementation(libs.bundles.shared.jvm.test.compose)

    androidTestImplementation(libs.bundles.shared.jvm.androidtest.compose)
}
