package com.fsck.k9.storage

import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.MigrationsHelper
import com.fsck.k9.mailstore.SchemaDefinitionFactory

class K9SchemaDefinitionFactory : SchemaDefinitionFactory {
    override val databaseVersion = StoreSchemaDefinition.DB_VERSION

    override fun createSchemaDefinition(migrationsHelper: MigrationsHelper): LockableDatabase.SchemaDefinition {
        return StoreSchemaDefinition(migrationsHelper)
    }
}
