package net.thunderbird.feature.account.settings.impl.domain.usecase

import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider

internal class FakeGeneralResourceProvider : ResourceProvider.GeneralResourceProvider {
    override val nameTitle: () -> String = { "Name" }
    override val nameDescription: () -> String? = { null }
    override val nameIcon: () -> ImageVector? = { null }
}
