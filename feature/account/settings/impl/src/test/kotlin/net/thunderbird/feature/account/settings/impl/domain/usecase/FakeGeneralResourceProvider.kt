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

    override val avatarTitle: () -> String = { "Avatar" }
    override val avatarDescription: () -> String? = { "Choose avatar type" }
    override val avatarOptionMonogram: () -> String = { "ProfileIndicatorMonogram" }
    override val avatarOptionImage: () -> String = { "ProfileIndicatorImage" }
    override val avatarOptionIcon: () -> String = { "ProfileIndicatorIcon" }

    override val nameTitle: () -> String = { "Name" }
    override val nameDescription: () -> String? = { "The name associated with your account." }
    override val nameIcon: () -> ImageVector? = { null }

    override val nameEmptyError: () -> String = { "Name cannot be empty" }
    override val nameTooLongError: () -> String = { "Name is too long" }
    override val monogramTitle: () -> String = { "Monogram" }
    override val monogramDescription: () -> String? = { "Enter two-letter initials to use as your avatar." }
    override val monogramEmptyError: () -> String = { "Monogram cannot be empty" }
    override val monogramTooLongError: () -> String = { "Monogram is too long" }

    override val colorTitle: () -> String = { "Color" }
    override val colorDescription: () -> String? = { null }
    override val colorIcon: () -> ImageVector? = { null }
    override val colors: ImmutableList<Int> = persistentListOf(0xFF0000, 0x00FF00, 0x0000FF)
}
