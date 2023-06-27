plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.setup"
    resourcePrefix = "account_setup_"

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
    implementation(projects.mail.protocols.imap)
    implementation(projects.mail.protocols.pop3)
    implementation(projects.mail.protocols.smtp)

    implementation(projects.feature.autodiscovery.service)
    implementation(projects.feature.account.oauth)

    testImplementation(projects.core.ui.compose.testing)
}
