plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.account.fake"
}

dependencies {
    api(projects.core.android.account)
    api(projects.feature.mail.account.fake)
}

codeCoverage {
    lineCoverage = 0
}
