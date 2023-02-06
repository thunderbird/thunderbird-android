package com.fsck.k9.ui.helper

import org.koin.dsl.module

val helperUiModule = module {
    factory<ContactNameProvider> { RealContactNameProvider(contacts = get()) }
    factory { AddressFormatterProvider(contactNameProvider = get(), resources = get()) }
}
