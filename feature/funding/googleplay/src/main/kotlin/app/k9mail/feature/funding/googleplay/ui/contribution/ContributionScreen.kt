package app.k9mail.feature.funding.googleplay.ui.contribution

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.funding.googleplay.R
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ContributionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<ContributionViewModel>(),
) {
    val activity = LocalActivity.current as ComponentActivity
    val context = LocalContext.current

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is ContributionContract.Effect.ManageSubscription -> {
                context.startActivity(
                    getManageSubscriptionIntent(
                        productId = effect.productId,
                        packageName = context.packageName,
                    ),
                )
            }

            is ContributionContract.Effect.PurchaseContribution -> {
                effect.startPurchaseFlow(activity)
            }
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBarWithBackButton(
                title = stringResource(R.string.funding_googleplay_contribution_title),
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        ContributionContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

private const val SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions"

private fun getManageSubscriptionIntent(
    productId: String,
    packageName: String,
): Intent {
    val uri = Uri.parse(SUBSCRIPTION_URL)
        .buildUpon()
        .appendQueryParameter("sku", productId)
        .appendQueryParameter("package", packageName)
        .build()

    return Intent(Intent.ACTION_VIEW, uri)
}
