package net.thunderbird.feature.account.settings.impl.domain.usecase

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider

internal class FakeGeneralResourceProvider : ResourceProvider.GeneralResourceProvider {
    override fun profileUi(
        name: String,
        color: Int,
        avatar: Avatar?,
    ): @Composable ((Modifier) -> Unit) = { }

    override val nameTitle: () -> String = { "Name" }
    override val nameDescription: () -> String? = { null }
    override val nameIcon: () -> ImageVector? = { null }

    override val nameEmptyError: () -> String = { "Name cannot be empty" }
    override val nameTooLongError: () -> String = { "Name is too long" }
    override val monogramTitle: () -> String = { "Monogram" }
    override val monogramEmptyError: () -> String = { "Monogram cannot be empty" }
    override val monogramTooLongError: () -> String = { "Monogram is too long" }

    override val colorTitle: () -> String = { "Color" }
    override val colorDescription: () -> String? = { null }
    override val colorIcon: () -> ImageVector? = { null }
    override val colors: ImmutableList<Int> = persistentListOf(0xFF0000, 0x00FF00, 0x0000FF)

    override val profileIndicatorIcon: () -> String = { "ProfileIndicatorIcon" }
    override val profileIndicatorImage: () -> String = { "ProfileIndicatorImage" }
    override val profileIndicatorMonogram: () -> String = { "ProfileIndicatorMonogram" }
    override val profileIndicatorTitle: () -> String = { "Profile Indicator" }
}
