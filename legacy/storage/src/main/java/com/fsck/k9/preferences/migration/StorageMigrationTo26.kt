package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.EmailAddressValidator

/**
 * Make sure identities are using a syntactically valid email address.
 *
 * Previously, we didn't validate input in the "Composition defaults" screen, and so there was no guarantee that the
 * `email` values contained a valid email address.
 */
class StorageMigrationTo26(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    private val emailAddressValidator = EmailAddressValidator()

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

            val trimmedEmail = email.trim()
            val newEmail = if (emailAddressValidator.isValidAddressOnly(trimmedEmail)) {
                trimmedEmail
            } else {
                "please.edit@invalid"
            }

            migrationsHelper.writeValue(db, "$accountUuid.email.$identityIndex", newEmail)

            identityIndex++
        }
    }
}
