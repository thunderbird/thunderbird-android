package com.fsck.k9.storage

import com.fsck.k9.mailstore.SchemaDefinitionFactory
import org.koin.dsl.module

val storageModule = module {
    single<SchemaDefinitionFactory> { K9SchemaDefinitionFactory() }
}
