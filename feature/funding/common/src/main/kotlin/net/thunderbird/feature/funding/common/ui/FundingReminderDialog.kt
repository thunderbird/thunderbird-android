package net.thunderbird.feature.funding.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.card.CardFilled
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextHeadlineSmall
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevicesWithBackground
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.funding.common.R
import androidx.compose.ui.window.Dialog as MaterialDialog

@Composable
fun FundingReminderDialog(
    appNameHeader: String,
    appNameBody: String,
    onDismissClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialDialog(
        onDismissRequest = onDismissClick,
        properties = DialogProperties(),
    ) {
        FundingReminderContent(
            appNameHeader,
            appNameBody,
            onDismissClick,
            onOkClick,
            modifier,
        )
    }
}

@Composable
private fun FundingReminderContent(
    appNameHeader: String,
    appNameBody: String,
    onDismissClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardFilled(
        shape = BoltTheme.shapes.large,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.padding(BoltTheme.spacings.triple),
        ) {
            Image(
                painter = painterResource(R.drawable.funding_roc_banner),
                contentDescription = stringResource(R.string.funding_reminder_roc_banner_content_description),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .defaultMinSize(minHeight = 90.dp)
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(BoltTheme.spacings.triple))
            TextHeadlineSmall(
                text = stringResource(R.string.funding_reminder_title, appNameHeader),
                textAlign = TextAlign.Center,
                color = BoltTheme.colors.secondary,
            )
            Spacer(modifier = Modifier.height(BoltTheme.spacings.double))
            TextBodyMedium(
                text = stringResource(R.string.funding_reminder_appeal_content, appNameBody),
            )
            Spacer(modifier = Modifier.height(BoltTheme.spacings.triple))
            Row(
                horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ButtonText(
                    text = stringResource(R.string.funding_reminder_decline),
                    onClick = onDismissClick,
                    modifier = Modifier,
                    enabled = true,
                    color = BoltTheme.colors.secondary,
                )
                ButtonText(
                    text = stringResource(R.string.funding_reminder_donate),
                    onClick = onOkClick,
                    modifier = Modifier,
                    enabled = true,
                    color = BoltTheme.colors.secondary,
                )
            }
        }
    }
}

@Composable
@PreviewDevicesWithBackground
private fun FundingReminderContentPreview() {
    PreviewWithTheme {
        FundingReminderContent(
            appNameHeader = "Thunderbird",
            appNameBody = "Thunderbird for Android",
            onDismissClick = {},
            onOkClick = {},
        )
    }
}
