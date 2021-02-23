package com.fsck.k9.mailstore

import com.fsck.k9.controller.MessageReference
import java.lang.RuntimeException

class MessageNotFoundException(val messageReference: MessageReference) :
    RuntimeException("Message not found: $messageReference")
