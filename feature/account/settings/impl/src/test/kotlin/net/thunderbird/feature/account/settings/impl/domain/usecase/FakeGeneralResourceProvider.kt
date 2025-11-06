package net.thunderbird.feature.account.settings.impl.domain.usecase

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider

internal class FakeGeneralResourceProvider : ResourceProvider.GeneralResourceProvider {
    override fun profileUi(
        name: String,
        color: Int,
        avatar: AccountAvatar?,
    ): @Composable ((Modifier) -> Unit) = { }

    override val nameTitle: () -> String = { "Name" }
    override val nameDescription: () -> String? = { null }
    override val nameIcon: () -> ImageVector? = { null }

    override val colorTitle: () -> String = { "Color" }
    override val colorDescription: () -> String? = { null }
    override val colorIcon: () -> ImageVector? = { null }
    override val colors: ImmutableList<Int> = persistentListOf(0xFF0000, 0x00FF00, 0x0000FF)

    override val profileIndicatorIcon: () -> String = { "ProfileIndicatorIcon" }
    override val profileIndicatorImage: () -> String = { "ProfileIndicatorImage" }
    override val profileIndicatorMonogram: () -> String = { "ProfileIndicatorMonogram" }
    override val profileIndicatorTitle: () -> String = { "Profile Indicator" }
}
