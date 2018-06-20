package com.fsck.k9.autocrypt

import org.koin.dsl.module.applicationContext

val autocryptModule = applicationContext {
    bean { AutocryptTransferMessageCreator(get()) }
}