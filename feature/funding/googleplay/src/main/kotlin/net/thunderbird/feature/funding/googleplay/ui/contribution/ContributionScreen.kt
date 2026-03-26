package net.thunderbird.feature.funding.googleplay.ui.contribution

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.funding.googleplay.R
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel
import org.koin.androidx.compose.koinViewModel
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.Event as PurchaseEvent

@Composable
internal fun ContributionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<ContributionViewModel>(),
) {
    val context = LocalContext.current

    val listState = viewModel.listState.collectAsStateWithLifecycle()
    val purchaseState = viewModel.purchaseState.collectAsStateWithLifecycle()

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.ManageSubscription -> {
                context.startActivity(
                    getManageSubscriptionIntent(
                        contributionId = effect.contributionId,
                        packageName = context.packageName,
                    ),
                )
            }
        }
    }

    BackHandler {
        onBack()
    }

    OnResume {
        viewModel.event(Event.Purchase(PurchaseEvent.RefreshPurchase))
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
            listState = listState.value,
            purchaseState = purchaseState.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

private const val SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions"

private fun getManageSubscriptionIntent(
    contributionId: ContributionId,
    packageName: String,
): Intent {
    val uri = SUBSCRIPTION_URL.toUri()
        .buildUpon()
        .appendQueryParameter("sku", contributionId.value)
        .appendQueryParameter("package", packageName)
        .build()

    return Intent(Intent.ACTION_VIEW, uri)
}

@Composable
fun OnResume(
    onResume: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, onResume) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
