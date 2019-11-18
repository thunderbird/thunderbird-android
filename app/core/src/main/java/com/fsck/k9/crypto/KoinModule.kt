package com.fsck.k9.crypto

import org.koin.dsl.module
import org.openintents.openpgp.util.OpenPgpApi
import org.sufficientlysecure.keychain.remote.OpenPgpService

val openPgpModule = module {
    single { OpenPgpService(get()) }
    single { OpenPgpApi(get(), get()) }
}
