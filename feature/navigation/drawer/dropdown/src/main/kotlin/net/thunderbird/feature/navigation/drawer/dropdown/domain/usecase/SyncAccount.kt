package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import android.content.Context
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase

internal class SyncAccount(
    private val messagingController: MessagingControllerMailChecker,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : UseCase.SyncAccount {
    override fun invoke(accountId: AccountId): Flow<Result<Unit>> = callbackFlow {
        val listener = object : SimpleMessagingListener() {
            override fun checkMailFinished(context: Context?, accountId: AccountId?) {
                trySend(Result.success(Unit))
                close()
            }
        }

        messagingController.checkMail(
            accountId = accountId,
            ignoreLastCheckedTime = true,
            useManualWakeLock = true,
            notify = true,
            listener = listener,
        )

        awaitClose()
    }.flowOn(coroutineContext)
}
