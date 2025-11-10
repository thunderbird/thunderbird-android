package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.baseline.withBaseline
import app.k9mail.core.ui.compose.common.resources.annotatedStringResource
import app.k9mail.core.ui.compose.common.text.bold
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconsWithBaseline
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.server.certificate.R
import app.k9mail.feature.account.server.certificate.domain.entity.FormattedServerCertificateError
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.State
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import org.koin.compose.koinInject

@Composable
internal fun ServerCertificateErrorContent(
    innerPadding: PaddingValues,
    state: State,
    scrollState: ScrollState,
) {
    ResponsiveWidthContainer(modifier = Modifier.padding(innerPadding)) { contentPadding ->
        Column(
            modifier = Modifier.verticalScroll(scrollState).padding(contentPadding),
        ) {
            CertificateErrorOverview(state)

            AnimatedContent(
                targetState = state.isShowServerCertificate,
                label = "ServerCertificateViewVisibility",
            ) { isShowServerCertificate ->
                if (isShowServerCertificate) {
                    ServerCertificateView(
                        serverCertificateProperties = state.certificateError!!.serverCertificateProperties,
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateErrorOverview(state: State) {
    Column(
        modifier = Modifier.padding(all = MainTheme.spacings.double),
    ) {
        WarningTitle()
        TextTitleMedium(stringResource(R.string.account_server_certificate_unknown_error_subtitle))

        Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))

        state.certificateError?.let { certificateError ->
            CertificateErrorDescription(certificateError)
        }
    }
}

@Composable
private fun WarningTitle() {
    Row {
        val warningIcon = IconsWithBaseline.Filled.warning
        val iconSize = MainTheme.sizes.medium
        val iconScalingFactor = iconSize / warningIcon.image.defaultHeight
        val iconBaseline = warningIcon.baseline * iconScalingFactor

        Icon(
            imageVector = warningIcon.image,
            tint = MainTheme.colors.warning,
            modifier = Modifier
                .padding(end = MainTheme.spacings.default)
                .requiredSize(iconSize)
                .withBaseline(iconBaseline)
                .alignByBaseline(),
        )
        TextHeadlineMedium(
            text = stringResource(R.string.account_server_certificate_warning_title),
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun CertificateErrorDescription(
    certificateError: FormattedServerCertificateError,
    serverNameFormatter: ServerNameFormatter = koinInject(),
) {
    TextBodyLarge(
        text = annotatedStringResource(
            id = R.string.account_server_certificate_unknown_error_description_format,
            serverNameFormatter.format(certificateError.hostname).bold(),
        ),
    )
}
