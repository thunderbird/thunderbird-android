package com.fsck.k9.storage

import com.fsck.k9.mailstore.SchemaDefinitionFactory
import org.koin.dsl.module.applicationContext

val storageModule = applicationContext {
    bean { K9SchemaDefinitionFactory() as SchemaDefinitionFactory }
}
