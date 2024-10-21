package app.k9mail.feature.funding.googleplay.data.remote

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Starts the billing client connection.
 *
 * Kotlin coroutines are used to suspend the coroutine until the connection is established.
 */
internal suspend fun BillingClient.startConnection(): BillingResult = suspendCancellableCoroutine { continuation ->
    startConnection(
        object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                continuation.resume(billingResult)
            }

            override fun onBillingServiceDisconnected() {
                continuation.resume(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED)
                        .setDebugMessage("Service disconnected: onBillingServiceDisconnected")
                        .build(),
                )
            }
        },
    )
}
