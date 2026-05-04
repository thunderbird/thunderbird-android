package net.thunderbird.legacy.core.mailstore.folder

import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider

class FakeLocalMessageUidPrefixProvider: LocalMessageUidPrefixProvider {
    override fun get(): String  = "FAKE"
}
