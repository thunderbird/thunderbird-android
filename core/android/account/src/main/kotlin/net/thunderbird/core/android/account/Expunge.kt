package net.thunderbird.core.android.account

import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy

enum class Expunge {
    EXPUNGE_IMMEDIATELY,
    EXPUNGE_MANUALLY,
    EXPUNGE_ON_POLL,
    ;

    fun toBackendExpungePolicy(): ExpungePolicy = when (this) {
        EXPUNGE_IMMEDIATELY -> ExpungePolicy.IMMEDIATELY
        EXPUNGE_MANUALLY -> ExpungePolicy.MANUALLY
        EXPUNGE_ON_POLL -> ExpungePolicy.ON_POLL
    }
}
