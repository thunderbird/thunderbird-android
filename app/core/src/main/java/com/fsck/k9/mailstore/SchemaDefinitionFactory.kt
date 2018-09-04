package com.fsck.k9.mailstore

import com.fsck.k9.mailstore.LockableDatabase.SchemaDefinition

interface SchemaDefinitionFactory {
    val databaseVersion: Int

    fun createSchemaDefinition(migrationsHelper: MigrationsHelper): SchemaDefinition
}
