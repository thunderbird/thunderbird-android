package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.backend.BackendManager

interface ControllerExtension {
    fun init(controller: MessagingController, backendManager: BackendManager, controllerInternals: ControllerInternals)

    interface ControllerInternals {
        fun put(description: String, listener: MessagingListener?, runnable: Runnable)
        fun putBackground(description: String, listener: MessagingListener?, runnable: Runnable)
    }
}
