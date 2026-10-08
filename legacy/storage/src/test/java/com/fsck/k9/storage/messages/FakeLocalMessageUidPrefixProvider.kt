package com.fsck.k9.storage.messages

import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider

class FakeLocalMessageUidPrefixProvider : LocalMessageUidPrefixProvider {
    override fun get(): String = "K9LOCAL:"
}
