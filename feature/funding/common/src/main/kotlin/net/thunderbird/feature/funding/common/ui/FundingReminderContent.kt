package net.thunderbird.feature.funding.common.ui

import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import net.thunderbird.components.ui.bolt.PreviewLightDarkLandscape
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.card.CardFilled
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextHeadlineSmall
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.funding.common.R
import androidx.compose.ui.window.Dialog as MaterialDialog

@Composable
fun FundingReminderContent(
    appNameHeader: String,
    appNameBody: String,
    onClickDismiss: () -> Unit,
    onClickOk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialDialog(
        onDismissRequest = onClickDismiss,
        properties = DialogProperties(),
    ) {
        CardFilled(
            shape = BoltTheme.shapes.large,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.padding(BoltTheme.spacings.triple),
            ) {
                Image(
                    painter = painterResource(R.drawable.funding_roc_banner),
                    contentDescription = stringResource(R.string.funding_reminder_roc_banner_content_description),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 120.dp)
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
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ButtonText(
                        text = stringResource(R.string.funding_reminder_decline),
                        onClick = onClickDismiss,
                        modifier = Modifier,
                        enabled = true,
                        color = BoltTheme.colors.secondary,
                    )
                    ButtonText(
                        text = stringResource(R.string.funding_reminder_donate),
                        onClick = onClickOk,
                        modifier = Modifier,
                        enabled = true,
                        color = BoltTheme.colors.secondary,
                    )
                }
            }
        }
    }
}
