package app.k9mail.feature.account.setup.ui.options.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SwitchInput
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.item.defaultHeadlineItemPadding
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State

@Suppress("LongMethod")
@Composable
internal fun SyncOptionsContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    appName: String,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("SyncOptionsContent")
            .consumeWindowInsets(contentPadding)
            .padding(contentPadding)
            .then(modifier),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            item {
                AppTitleTopHeader(
                    title = appName,
                )
            }

            item {
                TextLabelSmall(
                    text = stringResource(id = R.string.account_setup_options_section_sync_options),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(defaultHeadlineItemPadding()),
                )
            }

            item {
                SelectInput(
                    options = EmailCheckFrequency.all(),
                    optionToStringTransformation = { it.toResourceString(resources) },
                    selectedOption = state.checkFrequency,
                    onOptionChange = { onEvent(Event.OnCheckFrequencyChanged(it)) },
                    label = stringResource(id = R.string.account_setup_options_account_check_frequency_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                SelectInput(
                    options = EmailDisplayCount.all(),
                    optionToStringTransformation = { it.toResourceString(resources) },
                    selectedOption = state.messageDisplayCount,
                    onOptionChange = { onEvent(Event.OnMessageDisplayCountChanged(it)) },
                    label = stringResource(id = R.string.account_setup_options_email_display_count_label),
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                SwitchInput(
                    text = stringResource(id = R.string.account_setup_options_show_notifications_label),
                    checked = state.showNotification,
                    onCheckedChange = { onEvent(Event.OnShowNotificationChanged(it)) },
                    contentPadding = defaultItemPadding(),
                )
            }

            item {
                Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
            }
        }
    }
}
