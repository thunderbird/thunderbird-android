package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mail.Address

/**
 * Clean up [Identity][com.fsck.k9.Identity] properties stored in the database
 *
 * Previously, we didn't validate input in the "Edit identity" screen, and so there was no guarantee that the `email`
 * and `replyTo` values contained a valid email address.
 *
 * Additionally, we now rewrite blank values in `description`, `name`, and `replyTo` to `null`.
 */
class StorageMigrationTo20(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun fixIdentities() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            fixIdentitiesInAccount(accountUuid)
        }
    }

    private fun fixIdentitiesInAccount(accountUuid: String) {
        var identityIndex = 0

        while (true) {
            val email = migrationsHelper.readValue(db, "$accountUuid.email.$identityIndex") ?: break
            val description = migrationsHelper.readValue(db, "$accountUuid.description.$identityIndex")
            val name = migrationsHelper.readValue(db, "$accountUuid.name.$identityIndex")
            val replyTo = migrationsHelper.readValue(db, "$accountUuid.replyTo.$identityIndex")

            val newDescription = description?.takeUnless { it.isBlank() }
            val newName = name?.takeUnless { it.isBlank() }

            val emailAddress = Address.parse(email).firstOrNull()
            val newEmail = emailAddress?.address ?: "please.fix@invalid"

            val replyToAddress = Address.parse(replyTo).firstOrNull()
            val newReplyTo = replyToAddress?.address

            migrationsHelper.writeValue(db, "$accountUuid.description.$identityIndex", newDescription)
            migrationsHelper.writeValue(db, "$accountUuid.name.$identityIndex", newName)
            migrationsHelper.writeValue(db, "$accountUuid.email.$identityIndex", newEmail)
            migrationsHelper.writeValue(db, "$accountUuid.replyTo.$identityIndex", newReplyTo)

            identityIndex++
        }
    }
}
