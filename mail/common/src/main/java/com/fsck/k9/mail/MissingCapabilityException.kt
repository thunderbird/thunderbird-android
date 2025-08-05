package com.fsck.k9.mail
import net.thunderbird.core.common.exception.MessagingException

class MissingCapabilityException(
    val capabilityName: String,
) : MessagingException("Missing capability: $capabilityName", true)