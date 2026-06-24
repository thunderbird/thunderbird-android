plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "org.openintents.openpgp"

    buildFeatures {
        aidl = true
    }

    // Suppress deprecation annotation warnings for AIDL-generated code
    // AIDL files cannot use Java annotations, so generated code lacks @Deprecated annotations
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf(
            "-Xlint:-dep-ann"  // Suppress "deprecated element not annotated with @Deprecated" warnings
        ))
    }
}

dependencies {
    api(libs.androidx.lifecycle.common)
    api(libs.androidx.preference)
    api(libs.androidx.fragment)

    implementation(projects.core.logging.implLegacy)
    implementation(libs.androidx.annotation)
}
