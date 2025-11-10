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
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase

class SyncAllAccounts(
    private val messagingController: MessagingControllerMailChecker,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : UseCase.SyncAllAccounts {
    override fun invoke(): Flow<Result<Unit>> = callbackFlow {
        val listener = object : SimpleMessagingListener() {
            override fun checkMailFinished(context: Context?, account: LegacyAccountDto?) {
                trySend(Result.success(Unit))
                close()
            }
        }

        messagingController.checkMail(
            account = null,
            ignoreLastCheckedTime = true,
            useManualWakeLock = true,
            notify = true,
            listener = listener,
        )

        awaitClose()
    }.flowOn(coroutineContext)
}
