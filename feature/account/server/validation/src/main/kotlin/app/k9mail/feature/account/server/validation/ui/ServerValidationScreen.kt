package app.k9mail.feature.account.server.validation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorScreen
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Effect
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.ViewModel

@Suppress("ViewModelForwarding")
@Composable
fun ServerValidationScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    brandNameProvider: BrandNameProvider,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onNext()
            is Effect.NavigateBack -> onBack()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.LoadAccountStateAndValidate)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    if (state.value.error is ServerValidationContract.Error.CertificateError) {
        ServerCertificateErrorScreen(
            onCertificateAccepted = { dispatch(Event.OnCertificateAccepted) },
            onBack = { dispatch(Event.OnBackClicked) },
            modifier = modifier,
        )
    } else {
        if (title != null) {
            ServerValidationToolbarScreen(
                title = title,
                viewModel = viewModel,
                modifier = modifier,
            )
        } else {
            ServerValidationMainScreen(
                viewModel = viewModel,
                brandNameProvider = brandNameProvider,
                modifier = modifier,
            )
        }
    }
}
