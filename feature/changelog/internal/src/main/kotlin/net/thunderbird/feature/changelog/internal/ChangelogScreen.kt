package net.thunderbird.feature.changelog.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
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
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.components.ui.bolt.atom.Checkbox
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.DividerVertical
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.card.CardFilled
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge
import net.thunderbird.components.ui.bolt.atom.text.TextLabelMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.organism.TopAppBar
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
internal fun ChangelogScreen(
    releaseItems: ImmutableList<ReleaseUiModel>,
    showRecentChanges: Boolean,
    onShowRecentChangesCheck: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.changelog_title),
                navigationIcon = {
                    ButtonIcon(
                        onClick = onBack,
                        imageVector = Icons.Outlined.ArrowBack,
                    )
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            Column {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    items(
                        items = releaseItems,
                        key = { it.version },
                    ) { releaseItem ->
                        Spacer(modifier = Modifier.height(BoltTheme.spacings.triple))
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
}

@Composable
private fun ReleaseComposable(
    releaseItem: ReleaseUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BoltTheme.spacings.double)
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
                Column(modifier = Modifier.padding(horizontal = BoltTheme.spacings.double)) {
                    releaseItem.changes.forEach { changeGroup ->
                        val color = when (changeGroup.key) {
                            ChangeType.NEW.name -> BoltTheme.colors.onSuccessContainer
                            ChangeType.FIXED.name -> BoltTheme.colors.onWarningContainer
                            else -> BoltTheme.colors.onInfoContainer
                        }
                        val backgroundColor = when (changeGroup.key) {
                            ChangeType.NEW.name -> BoltTheme.colors.successContainer
                            ChangeType.FIXED.name -> BoltTheme.colors.warningContainer
                            else -> BoltTheme.colors.infoContainer
                        }

                        ChangeGroupHeader(changeGroup = changeGroup, color = color, backgroundColor = backgroundColor)

                        Spacer(modifier = Modifier.height(BoltTheme.spacings.half))

                        changeGroup.value.forEach { change ->
                            Change(change = change, color = color)
                            Spacer(modifier = Modifier.height(BoltTheme.spacings.default))
                        }
                        Spacer(modifier = Modifier.height(BoltTheme.spacings.default))
                    }
                    Spacer(modifier = Modifier.height(BoltTheme.spacings.default))
                }
            }
        }
    }
}

@Composable
fun ReleaseItemTimeline(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(BoltTheme.spacings.triple)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(BoltTheme.spacings.default))
        TimelineDot(
            color = BoltTheme.colors.outlineVariant,
            size = BoltTheme.spacings.oneHalf,
        )
        Spacer(modifier = Modifier.height(BoltTheme.spacings.default))
        DividerVertical(
            modifier = Modifier
                .padding(horizontal = BoltTheme.spacings.default)
                .weight(1.0f),
            thickness = BoltTheme.spacings.quarter,
        )
    }
}

@Suppress("MultipleEmitters")
@Composable
fun ReleaseItemHeader(
    release: String,
    isLatest: Boolean,
    date: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(
            end = BoltTheme.spacings.default,
            bottom = BoltTheme.spacings.half,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextTitleLarge(
            text = release.bold(),
            color = BoltTheme.colors.onSurface,
        )
        if (isLatest) {
            Spacer(Modifier.width(BoltTheme.spacings.default))
            Box(
                modifier = Modifier
                    .background(
                        color = BoltTheme.colors.outlineVariant.copy(alpha = 0.1f),
                        shape = BoltTheme.shapes.extraSmall,
                    )
                    .border(
                        width = 1.dp,
                        color = BoltTheme.colors.outlineVariant,
                        shape = BoltTheme.shapes.extraSmall,
                    ),
            ) {
                TextLabelSmall(
                    text = stringResource(R.string.changelog_latest_label_text),
                    modifier = Modifier.padding(
                        horizontal = BoltTheme.spacings.double,
                        vertical = BoltTheme.spacings.half,
                    ),
                )
            }
        }
    }

    TextLabelMedium(
        modifier = Modifier.padding(bottom = BoltTheme.spacings.double),
        text = date,
        color = BoltTheme.colors.onSurfaceVariant,
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
                top = BoltTheme.spacings.double,
                bottom = BoltTheme.spacings.default,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = BoltTheme.shapes.extraSmall,
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
                    horizontal = BoltTheme.spacings.double,
                    vertical = BoltTheme.spacings.half,
                ),
            )
        }
        DividerHorizontal(
            modifier = Modifier
                .padding(horizontal = BoltTheme.spacings.default)
                .weight(1.0f),
            thickness = BoltTheme.spacings.quarter,
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
        Spacer(modifier = Modifier.width(BoltTheme.spacings.default))
        TextBodyMedium(
            text = change,
            color = BoltTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = BoltTheme.sizes.smaller,
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
            .padding(top = BoltTheme.spacings.half),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Dot,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(BoltTheme.sizes.smaller),
        )
    }
}

@Composable
private fun String.toLocalizedText() = when (this) {
    ChangeType.NEW.name -> stringResource(R.string.changelog_type_new_text)
    ChangeType.FIXED.name -> stringResource(R.string.changelog_type_fixed_text)
    else -> stringResource(R.string.changelog_type_changed_text)
}
