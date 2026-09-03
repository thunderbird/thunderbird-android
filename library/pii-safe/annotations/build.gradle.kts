plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

description = "Public constants (plugin id, CLI option names) shared between the PII-safe compiler plugin " +
    "and its Gradle subplugin."

kotlin {
    explicitApi()
    android {
        namespace = "net.thunderbird.piisafe.annotation"
    }
}
