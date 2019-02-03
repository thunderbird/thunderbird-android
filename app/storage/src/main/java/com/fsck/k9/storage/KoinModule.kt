package com.fsck.k9.storage

import com.fsck.k9.mailstore.SchemaDefinitionFactory
import org.koin.dsl.module.module

val storageModule = module {
    single { K9SchemaDefinitionFactory() as SchemaDefinitionFactory }
}
