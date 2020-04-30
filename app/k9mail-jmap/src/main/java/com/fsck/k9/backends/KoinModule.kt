package com.fsck.k9.backends

import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.jmap.JmapAccountDiscovery
import org.koin.dsl.module

val backendsModule = module {
    single {
        BackendManager(
                mapOf(
                        "imap" to get<ImapBackendFactory>(),
                        "pop3" to get<Pop3BackendFactory>(),
                        "webdav" to get<WebDavBackendFactory>(),
                        "jmap" to get<JmapBackendFactory>()
                ))
    }
    single { ImapBackendFactory(get(), get(), get(), get()) }
    single { Pop3BackendFactory(get(), get()) }
    single { WebDavBackendFactory(get(), get(), get()) }
    single { JmapBackendFactory(get(), get()) }
    factory { JmapAccountDiscovery() }
    factory { JmapAccountCreator(get(), get(), get(), get()) }
    single { OkHttpClientProvider() }
}
