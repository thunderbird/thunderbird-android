@file:JvmName("Preconditions")

package com.fsck.k9.controller

import app.k9mail.legacy.di.DI
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider

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
    val localMessageUidPrefix = DI.get(LocalMessageUidPrefixProvider::class.java).get()
    require(!uid.startsWith(localMessageUidPrefix)) { "Local UID found: $uid" }
}
