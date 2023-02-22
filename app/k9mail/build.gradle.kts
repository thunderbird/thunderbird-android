plugins {
    id("thunderbird.app.android")
}

if (rootProject.testCoverage) {
    apply plugin: 'jacoco'
}

dependencies {
    implementation project(":app:ui:legacy")
    implementation project(":app:ui:message-list-widget")
    implementation project(":app:core")
    implementation project(":app:storage")
    implementation project(":app:crypto-openpgp")
    implementation project(":backend:imap")
    implementation project(":backend:pop3")
    implementation project(":backend:webdav")
    debugImplementation project(":backend:demo")

    implementation libs.androidx.appcompat
    implementation libs.androidx.core.ktx
    implementation libs.androidx.work.ktx
    implementation libs.preferencex
    implementation libs.timber
    implementation libs.kotlinx.coroutines.core

    implementation libs.glide
    annotationProcessor libs.glide.compiler

    if (project.hasProperty('k9mail.enableLeakCanary') && project.property('k9mail.enableLeakCanary') == "true") {
        debugImplementation libs.leakcanary.android
    }

    // Required for DependencyInjectionTest to be able to resolve OpenPgpApiManager
    testImplementation project(':plugins:openpgp-api-lib:openpgp-api')

    testImplementation libs.robolectric
}

android {
    namespace 'com.fsck.k9'

    defaultConfig {
        applicationId "com.fsck.k9"
        testApplicationId "com.fsck.k9.tests"

        versionCode 35008
        versionName '6.509-SNAPSHOT'

        // Keep in sync with the resource string array 'supported_languages'
        resConfigs "in", "br", "ca", "cs", "cy", "da", "de", "et", "en", "en_GB", "es", "eo", "eu", "fr", "gd", "gl",
                "hr", "is", "it", "lv", "lt", "hu", "nl", "nb", "pl", "pt_PT", "pt_BR", "ru", "ro", "sq", "sk", "sl",
                "fi", "sv", "tr", "el", "be", "bg", "sr", "uk", "iw", "ar", "fa", "ml", "ko", "zh_CN", "zh_TW", "ja",
                "fy"
    }

    signingConfigs {
        release
    }

    buildTypes {
        release {
            if (project.hasProperty('storeFile')) {
                signingConfig signingConfigs.release
            }

            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'

            buildConfigField "String", "OAUTH_GMAIL_CLIENT_ID", "\"262622259280-hhmh92rhklkg2k1tjil69epo0o9a12jm.apps.googleusercontent.com\""
            buildConfigField "String", "OAUTH_YAHOO_CLIENT_ID", "\"dj0yJmk9aHNUb3d2MW5TQnpRJmQ9WVdrOWVYbHpaRWM0YkdnbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PWIz\""
            buildConfigField "String", "OAUTH_AOL_CLIENT_ID", "\"dj0yJmk9dUNqYXZhYWxOYkdRJmQ9WVdrOU1YQnZVRFZoY1ZrbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PWIw\""
            buildConfigField "String", "OAUTH_MICROSOFT_CLIENT_ID", "\"e647013a-ada4-4114-b419-e43d250f99c5\""
            buildConfigField "String", "OAUTH_MICROSOFT_REDIRECT_URI", "\"msauth://com.fsck.k9/Dx8yUsuhyU3dYYba1aA16Wxu5eM%3D\""

            manifestPlaceholders = ['appAuthRedirectScheme': 'com.fsck.k9']
        }

        debug {
            applicationIdSuffix ".debug"
            testCoverageEnabled rootProject.testCoverage

            minifyEnabled false

            buildConfigField "String", "OAUTH_GMAIL_CLIENT_ID", "\"262622259280-5qb3vtj68d5dtudmaif4g9vd3cpar8r3.apps.googleusercontent.com\""
            buildConfigField "String", "OAUTH_YAHOO_CLIENT_ID", "\"dj0yJmk9ejRCRU1ybmZjQlVBJmQ9WVdrOVVrZEViak4xYmxZbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PTZj\""
            buildConfigField "String", "OAUTH_AOL_CLIENT_ID", "\"dj0yJmk9cHYydkJkTUxHcXlYJmQ9WVdrOWVHZHhVVXN4VVV3bWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PTdm\""
            buildConfigField "String", "OAUTH_MICROSOFT_CLIENT_ID", "\"e647013a-ada4-4114-b419-e43d250f99c5\""
            buildConfigField "String", "OAUTH_MICROSOFT_REDIRECT_URI", "\"msauth://com.fsck.k9.debug/VZF2DYuLYAu4TurFd6usQB2JPts%3D\""

            manifestPlaceholders = ['appAuthRedirectScheme': 'com.fsck.k9.debug']
        }
    }

    packagingOptions {
        jniLibs {
            excludes += ['kotlin/**']
        }

        resources {
            excludes += [
                'META-INF/DEPENDENCIES',
                'META-INF/LICENSE',
                'META-INF/LICENSE.txt',
                'META-INF/NOTICE',
                'META-INF/NOTICE.txt',
                'META-INF/README',
                'META-INF/README.md',
                'META-INF/CHANGES',
                'LICENSE.txt',
                'META-INF/*.kotlin_module',
                'META-INF/*.version',
                'kotlin/**',
                'DebugProbesKt.bin'
            ]
        }
    }
}

if (project.hasProperty('k9mail.keyAlias')) {
    android.signingConfigs.release.keyAlias = project.property('k9mail.keyAlias')
}
if (project.hasProperty('k9mail.keyPassword')) {
    android.signingConfigs.release.keyPassword = project.property('k9mail.keyPassword')
}
if (project.hasProperty('k9mail.storeFile')) {
    android.signingConfigs.release.storeFile = file(project.property('k9mail.storeFile'))
}
if (project.hasProperty('k9mail.storePassword')) {
    android.signingConfigs.release.storePassword = project.property('k9mail.storePassword')
}
