package app.k9mail.feature.account.server.validation.ui.fake

import net.thunderbird.core.common.provider.BrandNameProvider

internal object FakeBrandNameProvider : BrandNameProvider {
    override val brandName: String = "Fake Brand Name"
}
