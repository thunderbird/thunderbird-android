package net.thunderbird.feature.mail.message.list.internal

import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider

private const val LOCAL_UID_PREFIX = "K9LOCAL:"

internal class DefaultLocalMessageUidPrefixProvider : LocalMessageUidPrefixProvider {
    override fun get(): String = LOCAL_UID_PREFIX
}
