package app.k9mail.feature.account.setup.ui.fake

import app.k9mail.core.common.provider.AppNameProvider

internal object FakeAppNameProvider : AppNameProvider {
    override val appName: String = "Fake App Name"
}
