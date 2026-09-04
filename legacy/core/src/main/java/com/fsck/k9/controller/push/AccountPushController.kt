package com.fsck.k9.controller.push

import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.fold
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.data.repository.PushFoldersQueryRepository

private const val TAG = "AccountPushController"

internal class AccountPushController(
    private val backendManager: BackendManager,
    private val pushFoldersQueryRepository: PushFoldersQueryRepository,
    private val backendPusherCallback: BackendPusherCallback,
    private val accountId: AccountId,
    private val logger: Logger,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    @Volatile
    private var backendPusher: BackendPusher? = null

    fun start() {
        logger.verbose(TAG) { "Starting push controller for account $accountId" }
        startBackendPusher()
        startListeningForPushFolders()
    }

    fun stop() {
        logger.verbose(TAG) { "Stopping push controller for account $accountId" }
        stopListeningForPushFolders()
        stopBackendPusher()
    }

    fun reconnect() {
        logger.verbose(TAG) { "Reconnecting push controller for account $accountId" }
        backendPusher?.reconnect()
    }

    private fun startBackendPusher() {
        val backend = backendManager.getBackend(accountId)
        backendPusher = backend.createPusher(backendPusherCallback).also { backendPusher ->
            backendPusher.start()
        }
    }

    private fun stopBackendPusher() {
        backendPusher?.stop()
        backendPusher = null
    }

    private fun startListeningForPushFolders() {
        coroutineScope.launch {
            pushFoldersQueryRepository.observeAllByAccountId(accountId)
                .collect { outcome ->
                    outcome.fold(
                        onSuccess = { remoteFolders ->
                            val folderServerIds = remoteFolders.map { it.serverId }
                            updatePushFolders(folderServerIds)
                        },
                        onFailure = {
                            logger.error { "Failed to start listening for push folders. Error: $it" }
                        },
                    )
                }
        }
    }

    private fun stopListeningForPushFolders() {
        coroutineScope.cancel()
    }

    private fun updatePushFolders(folderServerIds: List<String>) {
        logger.verbose(TAG) { "Updating push folders for account $accountId: $folderServerIds" }

        backendPusher?.updateFolders(folderServerIds)
    }
}
