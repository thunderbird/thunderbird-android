@file:JvmName("Preconditions")

package com.fsck.k9.controller

import com.fsck.k9.K9

fun <T : Any> requireNotNull(value: T?) {
    kotlin.requireNotNull(value)
}

fun requireValidUids(uidMap: Map<String?, String?>?) {
    kotlin.requireNotNull(uidMap)
    for ((sourceUid, destinationUid) in uidMap) {
        requireNotLocalUid(sourceUid)
        kotlin.requireNotNull(destinationUid)
    }
}

fun requireValidUids(uids: List<String?>?) {
    kotlin.requireNotNull(uids)
    for (uid in uids) {
        requireNotLocalUid(uid)
    }
}

private fun requireNotLocalUid(uid: String?) {
    kotlin.requireNotNull(uid)
    require(!uid.startsWith(K9.LOCAL_UID_PREFIX)) { "Local UID found: $uid" }
}
