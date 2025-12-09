package net.thunderbird.feature.account.avatar.ui

import android.media.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.AvatarIcon
import net.thunderbird.feature.account.avatar.AvatarIconCatalog

@Composable
@PreviewLightDark
internal fun AvatarPreview() {
    AvatarPreviewSetup {
        Avatar(
            avatar = Avatar.Monogram("AB"),
            color = Color.Red,
            size = AvatarSize.MEDIUM,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarSelectedPreview() {
    AvatarPreviewSetup {
        Avatar(
            avatar = Avatar.Monogram("AB"),
            color = Color.Red,
            size = AvatarSize.MEDIUM,
            selected = true,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarIconPreview() {
    AvatarPreviewSetup {
        Avatar(
            avatar = Avatar.Icon("person"),
            color = Color.Red,
            size = AvatarSize.MEDIUM,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarIconLargePreview() {
    AvatarPreviewSetup {
        Avatar(
            avatar = Avatar.Icon("person"),
            color = Color.Red,
            size = AvatarSize.LARGE,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarImagePreview() {
    AvatarPreviewSetup {
        Avatar(
            avatar = Avatar.Image(""),
            color = Color.Red,
            size = AvatarSize.MEDIUM,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarImageLargePreview() {
    AvatarPreviewSetup {
        Avatar(
            avatar = Avatar.Image(""),
            color = Color.Red,
            size = AvatarSize.LARGE,
        )
    }
}

@Composable
internal fun AvatarPreviewSetup(
    content: @Composable () -> Unit,
) {
    koinPreview {
        factory<AvatarIconCatalog<AvatarIcon<ImageVector>>> {
            FakeAvatarIconCatalog()
        }
    } WithContent {
        PreviewWithTheme {
            content()
        }
    }
}

private class FakeAvatarIconCatalog(
    override val defaultIcon: AvatarIcon<ImageVector> = object : AvatarIcon<ImageVector> {
        override val id: String = "person"
        override val icon: ImageVector = Icons.Outlined.Person
    },
) : AvatarIconCatalog<AvatarIcon<ImageVector>> {
    override fun get(id: String): AvatarIcon<ImageVector> = defaultIcon

    override fun all(): List<AvatarIcon<ImageVector>> = emptyList()

    override fun contains(id: String): Boolean = true
}
