package com.fsck.k9.mailstore

class MessageNotFoundException(val folderId: Long, val messageServerId: String) :
    RuntimeException("Message not found: $folderId:$messageServerId")
