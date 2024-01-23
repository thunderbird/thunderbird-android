plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.main"
    resourcePrefix = "onboarding_main_"

    buildTypes {
        debug {
            manifestPlaceholders["appAuthRedirectScheme"] = "FIXME: override this in your app project"
        }
        release {
            manifestPlaceholders["appAuthRedirectScheme"] = "FIXME: override this in your app project"
        }
    }
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.feature.onboarding.welcome)
    implementation(projects.feature.account.setup)
    implementation(projects.feature.settings.import)
    implementation(projects.feature.onboarding.permissions)
}
