import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs

fun CommonExtension<*, *, *, *>.configureSharedConfig() {
    compileSdk = ThunderbirdProjectConfig.androidSdkCompile

    defaultConfig {
        compileSdk = ThunderbirdProjectConfig.androidSdkCompile
        minSdk = ThunderbirdProjectConfig.androidSdkMin

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = ThunderbirdProjectConfig.javaVersion
        targetCompatibility = ThunderbirdProjectConfig.javaVersion
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

fun CommonExtension<*, *, *, *>.configureSharedComposeConfig(libs: LibrariesForLibs) {
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

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
