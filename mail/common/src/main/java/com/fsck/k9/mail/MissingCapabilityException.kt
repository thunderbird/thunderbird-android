package com.fsck.k9.mail

class MissingCapabilityException(
    val capabilityName: String,
) : MessagingException("Missing capability: $capabilityName", true)
