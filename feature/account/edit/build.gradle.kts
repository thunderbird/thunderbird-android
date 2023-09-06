plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.edit"
    resourcePrefix = "account_edit_"

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
    implementation(projects.core.common)

    implementation(projects.mail.common)

    implementation(projects.feature.account.common)
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.account.server.settings)
    implementation(projects.feature.account.server.certificate)
    implementation(projects.feature.account.server.validation)

    testImplementation(projects.core.ui.compose.testing)
}
