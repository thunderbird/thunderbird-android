package app.k9mail.feature.account.setup.ui

import net.thunderbird.core.common.provider.BrandNameProvider

internal object FakeBrandNameProvider : BrandNameProvider {
    override val brandName: String = "Fake Brand Name"
}
