package app.k9mail.feature.navigation.drawer.domain.usecase

import android.content.Context
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class SyncAllAccounts(
    private val messagingController: MessagingControllerMailChecker,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : UseCase.SyncAllAccounts {
    override fun invoke(): Flow<Result<Unit>> = callbackFlow {
        val listener = object : SimpleMessagingListener() {
            override fun checkMailFinished(context: Context?, account: Account?) {
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
