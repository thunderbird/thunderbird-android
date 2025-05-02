package net.thunderbird.feature.account.settings.impl.domain.usecase

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider

internal class FakeGeneralResourceProvider : ResourceProvider.GeneralResourceProvider {
    override fun profileUi(
        name: String,
        color: Int,
    ): @Composable ((Modifier) -> Unit) = { }

    override val nameTitle: () -> String = { "Name" }
    override val nameDescription: () -> String? = { null }
    override val nameIcon: () -> ImageVector? = { null }

    override val colorTitle: () -> String = { "Color" }
    override val colorDescription: () -> String? = { null }
    override val colorIcon: () -> ImageVector? = { null }
    override val colors: List<Int> = listOf(0xFF0000, 0x00FF00, 0x0000FF)
}
