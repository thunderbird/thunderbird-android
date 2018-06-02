package com.fsck.k9.backend

import org.koin.dsl.module.applicationContext

val backendModule = applicationContext {
    bean { BackendManager(
            mapOf<String, BackendFactory>(
                    "imap" to get<ImapBackendFactory>()
            ))
    }
    bean { ImapBackendFactory(get()) }
}
