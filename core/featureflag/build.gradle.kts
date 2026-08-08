plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.tb.featureflag.library)
}

featureFlag {
    catalog.set(layout.settingsDirectory.file("config/featureflag/thunderbird_mobile_featureflag.catalog.json"))
}

kotlin {
    android {
        namespace = "net.thunderbird.core.featureflag"
    }
}

codeCoverage {
    lineCoverage = 60
}
