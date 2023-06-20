plugins {
    id(ThunderbirdPlugins.App.android)
}

val testCoverageEnabled: Boolean by extra
if (testCoverageEnabled) {
    apply(plugin = "jacoco")
}

dependencies {
    implementation(projects.app.ui.legacy)
    implementation(projects.app.ui.messageListWidget)
    implementation(projects.app.core)
    implementation(projects.app.storage)
    implementation(projects.app.cryptoOpenpgp)
    implementation(projects.backend.imap)
    implementation(projects.backend.pop3)
    debugImplementation(projects.backend.demo)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.ktx)
    implementation(libs.preferencex)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    if (project.hasProperty("k9mail.enableLeakCanary") && project.property("k9mail.enableLeakCanary") == "true") {
        debugImplementation(libs.leakcanary.android)
    }

    // Required for DependencyInjectionTest to be able to resolve OpenPgpApiManager
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)

    testImplementation(libs.robolectric)
}

android {
    namespace = "com.fsck.k9"

    defaultConfig {
        applicationId = "com.fsck.k9"
        testApplicationId = "com.fsck.k9.tests"

        versionCode = 37007
        versionName = "6.708-SNAPSHOT"

        // Keep in sync with the resource string array "supported_languages"
        resourceConfigurations.addAll(
            listOf(
                "in", "br", "ca", "cs", "cy", "da", "de", "et", "en", "en_GB", "es", "eo", "eu", "fr", "gd", "gl",
                "hr", "is", "it", "lv", "lt", "hu", "nl", "nb", "pl", "pt_PT", "pt_BR", "ru", "ro", "sq", "sk", "sl",
                "fi", "sv", "tr", "el", "be", "bg", "sr", "uk", "iw", "ar", "fa", "ml", "ko", "zh_CN", "zh_TW", "ja",
                "fy",
            ),
        )

        buildConfigField("String", "CLIENT_ID_APP_NAME", "\"K-9 Mail\"")
    }

    signingConfigs {
        if (project.hasProperty("k9mail.keyAlias") &&
            project.hasProperty("k9mail.keyPassword") &&
            project.hasProperty("k9mail.storeFile") &&
            project.hasProperty("k9mail.storePassword")
        ) {
            create("release") {
                keyAlias = project.property("k9mail.keyAlias") as String
                keyPassword = project.property("k9mail.keyPassword") as String
                storeFile = file(project.property("k9mail.storeFile") as String)
                storePassword = project.property("k9mail.storePassword") as String
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { releaseSigningConfig ->
                signingConfig = releaseSigningConfig
            }

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )

            manifestPlaceholders["appAuthRedirectScheme"] = "com.fsck.k9"
        }

        debug {
            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = testCoverageEnabled
            enableAndroidTestCoverage = testCoverageEnabled

            isMinifyEnabled = false

            manifestPlaceholders["appAuthRedirectScheme"] = "com.fsck.k9.debug"
        }
    }

    packagingOptions {
        jniLibs {
            excludes += listOf("kotlin/**")
        }

        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/README",
                "META-INF/README.md",
                "META-INF/CHANGES",
                "LICENSE.txt",
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "kotlin/**",
                "DebugProbesKt.bin",
            )
        }
    }
}
