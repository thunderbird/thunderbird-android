package com.fsck.k9.ui.changelog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.text.bold
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.DividerVertical
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.card.CardFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import com.fsck.k9.ui.R
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme

@Composable
internal fun ChangelogScreen(
    releaseItems: ImmutableList<ReleaseUiModel>,
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
                    key = { it.version },
                ) { releaseItem ->
                    Spacer(modifier = Modifier.height(MainTheme.spacings.triple))
                    ReleaseComposable(
                        releaseItem = releaseItem,
                    )
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

                TextLabelMedium(
                    text = stringResource(R.string.changelog_show_recent_changes),
                )
            }
        }
    }
}

@Composable
private fun ReleaseComposable(
    releaseItem: ReleaseUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MainTheme.spacings.double)
            .height(IntrinsicSize.Min),
    ) {
        ReleaseItemTimeline()

        Column {
            ReleaseItemHeader(
                release = releaseItem.version,
                isLatest = releaseItem.isLatest,
                date = releaseItem.date ?: "",
            )
            CardFilled {
                Column(modifier = Modifier.padding(horizontal = MainTheme.spacings.double)) {
                    releaseItem.changes.forEach { changeGroup ->
                        val color = when (changeGroup.key) {
                            ChangeType.NEW.name -> MainTheme.colors.onSuccessContainer
                            ChangeType.FIXED.name -> MainTheme.colors.onWarningContainer
                            else -> MainTheme.colors.onInfoContainer
                        }
                        val backgroundColor = when (changeGroup.key) {
                            ChangeType.NEW.name -> MainTheme.colors.successContainer
                            ChangeType.FIXED.name -> MainTheme.colors.warningContainer
                            else -> MainTheme.colors.infoContainer
                        }

                        ChangeGroupHeader(changeGroup = changeGroup, color = color, backgroundColor = backgroundColor)

                        Spacer(modifier = Modifier.height(MainTheme.spacings.half))

                        changeGroup.value.forEach { change ->
                            Change(change = change, color = color)
                            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
                        }
                        Spacer(modifier = Modifier.height(MainTheme.spacings.default))
                    }
                    Spacer(modifier = Modifier.height(MainTheme.spacings.default))
                }
            }
        }
    }
}

@Composable
private fun ReleaseItemTimeline(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(MainTheme.spacings.triple)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        TimelineDot(
            color = MainTheme.colors.outlineVariant,
            size = MainTheme.spacings.oneHalf,
        )
        Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        DividerVertical(
            modifier = Modifier
                .padding(horizontal = MainTheme.spacings.default)
                .weight(1.0f),
            thickness = MainTheme.spacings.quarter,
        )
    }
}

@Suppress("MultipleEmitters")
@Composable
private fun ReleaseItemHeader(
    release: String,
    isLatest: Boolean,
    date: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(
            end = MainTheme.spacings.default,
            bottom = MainTheme.spacings.half,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextTitleLarge(
            text = release.bold(),
            color = MainTheme.colors.onSurface,
        )
        if (isLatest) {
            Spacer(Modifier.width(MainTheme.spacings.default))
            Box(
                modifier = Modifier
                    .background(
                        color = MainTheme.colors.outlineVariant.copy(alpha = 0.1f),
                        shape = MainTheme.shapes.extraSmall,
                    )
                    .border(
                        width = 1.dp,
                        color = MainTheme.colors.outlineVariant,
                        shape = MainTheme.shapes.extraSmall,
                    ),
            ) {
                TextLabelSmall(
                    text = stringResource(R.string.changelog_latest_label_text),
                    modifier = Modifier.padding(
                        horizontal = MainTheme.spacings.double,
                        vertical = MainTheme.spacings.half,
                    ),
                )
            }
        }
    }

    TextLabelMedium(
        modifier = Modifier.padding(bottom = MainTheme.spacings.double),
        text = date,
        color = MainTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun ChangeGroupHeader(
    changeGroup: Map.Entry<String, List<String>>,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(
                top = MainTheme.spacings.double,
                bottom = MainTheme.spacings.default,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = MainTheme.shapes.extraSmall,
                )
                .border(
                    color = color.copy(alpha = 0.5f),
                    width = 1.dp,
                ),
        ) {
            TextLabelLarge(
                text = changeGroup.key.toLocalizedText().bold(),
                color = color,
                modifier = Modifier.padding(
                    horizontal = MainTheme.spacings.double,
                    vertical = MainTheme.spacings.half,
                ),
            )
        }
        DividerHorizontal(
            modifier = Modifier
                .padding(horizontal = MainTheme.spacings.default)
                .weight(1.0f),
            thickness = MainTheme.spacings.quarter,
        )
    }
}

@Composable
private fun Change(
    change: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        ChangeDot(
            color = color,
        )
        Spacer(modifier = Modifier.width(MainTheme.spacings.default))
        TextBodyMedium(
            text = change,
            color = MainTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = MainTheme.sizes.smaller,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, shape = CircleShape),
    )
}

@Composable
private fun ChangeDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = MainTheme.spacings.half),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Dot,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(MainTheme.sizes.smaller),
        )
    }
}

@Composable
private fun String.toLocalizedText() = when (this) {
    ChangeType.NEW.name -> stringResource(R.string.changelog_type_new_text)
    ChangeType.FIXED.name -> stringResource(R.string.changelog_type_fixed_text)
    else -> stringResource(R.string.changelog_type_changed_text)
}
