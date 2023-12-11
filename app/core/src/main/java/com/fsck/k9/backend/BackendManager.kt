package com.fsck.k9.backend

import com.fsck.k9.Account
import com.fsck.k9.backend.api.Backend

interface BackendManager {
    fun getBackend(account: Account): Backend
    fun removeBackend(account: Account)
    fun addListener(listener: BackendChangedListener)
    fun removeListener(listener: BackendChangedListener)
}

fun interface BackendChangedListener {
    fun onBackendChanged(account: Account)
}
