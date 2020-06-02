@file:JvmName("Preconditions")
package com.fsck.k9.controller

import com.fsck.k9.K9

fun requireValidUids(uidMap: Map<String?, String?>?) {
    requireNotNull(uidMap)
    for ((sourceUid, destinationUid) in uidMap) {
        requireNotLocalUid(sourceUid)
        requireNotNull(destinationUid)
    }
}

fun requireValidUids(uids: List<String?>?) {
    requireNotNull(uids)
    for (uid in uids) {
        requireNotLocalUid(uid)
    }
}

private fun requireNotLocalUid(uid: String?) {
    requireNotNull(uid)
    require(!uid.startsWith(K9.LOCAL_UID_PREFIX)) { "Local UID found: $uid" }
}
