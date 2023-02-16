import com.android.build.api.dsl.CommonExtension

fun CommonExtension<*, *, *, *>.configureSharedConfig() {
    compileSdk = ThunderbirdProjectConfig.androidSdkCompile

    defaultConfig {
        compileSdk = ThunderbirdProjectConfig.androidSdkCompile
        minSdk = ThunderbirdProjectConfig.androidSdkMin

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = ThunderbirdProjectConfig.javaVersion
        targetCompatibility = ThunderbirdProjectConfig.javaVersion
    }

    lint {
        checkDependencies = true
        abortOnError = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
