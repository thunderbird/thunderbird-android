package app.k9mail.feature.account.setup.ui.validation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.setup.ui.servercertificate.CertificateErrorScreen
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Effect
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.ViewModel

@Composable
internal fun AccountValidationScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onNext()
            is Effect.NavigateBack -> onBack()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.LoadAccountSetupStateAndValidate)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    if (state.value.error is AccountValidationContract.Error.CertificateError) {
        CertificateErrorScreen(
            onCertificateAccepted = { dispatch(Event.OnCertificateAccepted) },
            onBack = { dispatch(Event.OnBackClicked) },
            modifier = modifier,
        )
    } else {
        AccountValidationMainScreen(
            viewModel = viewModel,
            modifier = modifier,
        )
    }
}
