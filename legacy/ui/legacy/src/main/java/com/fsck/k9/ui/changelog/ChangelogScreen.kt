package com.fsck.k9.ui.changelog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.card.CardFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import com.fsck.k9.ui.R
import de.cketti.changelog.ReleaseItem
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.theme2.MainTheme

@Composable
fun ChangelogScreen(
    releaseItems: ImmutableList<ReleaseItem>,
    showRecentChanges: Boolean,
    modifier: Modifier = Modifier,
    onShowRecentChangesCheck: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        Column {
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(
                    items = releaseItems,
                    key = { it.versionName },
                ) { releaseItem ->
                    ReleaseItemView(releaseItem)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShowRecentChangesCheck(showRecentChanges)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = showRecentChanges,
                    onCheckedChange = {
                        onShowRecentChangesCheck(it)
                    },
                )

                TextLabelSmall(
                    text = stringResource(R.string.changelog_show_recent_changes),
                )
            }
        }
    }
}

@Composable
fun ReleaseItemView(
    releaseItem: ReleaseItem,
    modifier: Modifier = Modifier,
) {
    val spacing = MainTheme.spacings.default

    CardFilled(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing)
            .padding(vertical = MainTheme.spacings.half),
    ) {
        Column(
            modifier = Modifier.padding(spacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextTitleLarge("v${releaseItem.versionName}")

                Spacer(modifier = Modifier.weight(1f))

                releaseItem.date?.let {
                    TextBodyMedium(text = it)
                }
            }

            DividerHorizontal(
                modifier = Modifier.padding(vertical = spacing).fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(spacing))

            releaseItem.changes.forEach { change ->

                Row(
                    verticalAlignment = Alignment.Top,
                ) {
                    Image(
                        painter = painterResource(id = getChangeSymbol(change)),
                        modifier = Modifier.size(size = MainTheme.sizes.icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MainTheme.colors.primary),
                    )

                    Spacer(modifier = Modifier.width(MainTheme.spacings.default))

                    TextTitleMedium(change)
                }
                Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            }
        }
    }
}

fun getChangeSymbol(change: String): Int {
    return when {
        change.contains("fix", ignoreCase = true) -> R.drawable.ic_fix
        change.contains("translation", ignoreCase = true) ||
            change.contains("translate", ignoreCase = true) -> R.drawable.ic_language
        change.contains("update", ignoreCase = true) -> R.drawable.ic_update
        else -> R.drawable.ic_star
    }
}
