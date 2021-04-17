package com.fsck.k9.mail.helper

import com.fsck.k9.mail.FetchProfile

fun fetchProfileOf(vararg items: FetchProfile.Item): FetchProfile {
    return FetchProfile().apply {
        for (item in items) {
            add(item)
        }
    }
}
