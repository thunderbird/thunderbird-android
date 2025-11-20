package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.funding.googleplay.R
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Composable
fun ContributionError(
    error: BillingError?,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (error) {
        is BillingError.DeveloperError,
        is BillingError.PurchaseFailed,
        is BillingError.ServiceDisconnected,
        is BillingError.UnknownError,
        -> ContributionErrorView(
            title = mapErrorToTitle(error),
            description = error.message,
            onDismissClick = onDismissClick,
            modifier = modifier,
        )

        is BillingError.UserCancelled -> Unit // could be ignored
        null -> Unit
    }
}

@Composable
private fun ContributionErrorView(
    title: String,
    description: String,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showDetails = remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = MainTheme.colors.errorContainer,
        shape = MainTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MainTheme.spacings.double,
                vertical = MainTheme.spacings.default,
            ),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
            ) {
                TextBodyLarge(
                    text = title,
                    color = MainTheme.colors.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                if (description.isNotEmpty()) {
                    Icon(
                        imageVector = if (showDetails.value) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(R.string.funding_googleplay_contribution_error_show_more),
                        modifier = Modifier
                            .clickable { showDetails.value = !showDetails.value }
                            .padding(MainTheme.spacings.quarter),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.funding_googleplay_contribution_error_dismiss_button),
                    modifier = Modifier
                        .clickable { onDismissClick() }
                        .padding(MainTheme.spacings.quarter),
                )
            }

            AnimatedVisibility(visible = showDetails.value) {
                TextBodySmall(
                    text = description,
                    color = MainTheme.colors.onErrorContainer,
                )
            }
        }
    }
}

@Composable
internal fun mapErrorToTitle(error: BillingError): String {
    return when (error) {
        is BillingError.PurchaseFailed -> {
            stringResource(R.string.funding_googleplay_contribution_error_purchase_failed)
        }

        is BillingError.ServiceDisconnected -> {
            stringResource(R.string.funding_googleplay_contribution_error_service_disconnected)
        }

        is BillingError.DeveloperError,
        is BillingError.UnknownError,
        -> {
            stringResource(R.string.funding_googleplay_contribution_error_unknown)
        }

        is BillingError.UserCancelled -> error("User cancelled not supported")
    }
}
