package net.thunderbird.app.common.account.data

import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId

class DefaultAccountDisplayOrderManager(
    private val storageProvider: StorageProvider,
) : AccountDisplayOrderManager {

    private val storage: Storage
        get() = storageProvider.loadLatestStorage()

    override fun moveToPosition(accountId: AccountId, newPosition: Int) {
        val accountUuids = storage.getStringOrDefault("accountUuids", "")
            .split(",")
            .filter { it.isNotEmpty() }

        val oldPosition = accountUuids.indexOf(accountId.toString())
        if (oldPosition == -1) return

        val mutableUuids = accountUuids.toMutableList()
        mutableUuids.removeAt(oldPosition)

        val targetPosition = newPosition.coerceIn(0, mutableUuids.size)
        if (oldPosition == targetPosition) return

        mutableUuids.add(targetPosition, accountId.toString())

        val newAccountUuidsString = mutableUuids.joinToString(separator = ",")
        val editor = storageProvider.createStorageEditor()
        editor.putString("accountUuids", newAccountUuidsString)
        check(editor.commit()) { "Failed to update account display order" }
    }
}
