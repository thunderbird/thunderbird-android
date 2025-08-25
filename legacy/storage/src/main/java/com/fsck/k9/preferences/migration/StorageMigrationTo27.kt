package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

/**
 * Migration to add avatar monograms for accounts that have the MONOGRAM avatar type
 * and do not have an existing avatar monogram.
 */
class StorageMigrationTo27(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun addAvatarMonogram() {
        val accountUuidsValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsValue.split(",")
        for (accountUuid in accountUuids) {
            addAvatarMonogramToAccount(accountUuid)
        }
    }

    private fun addAvatarMonogramToAccount(accountUuid: String) {
        val avatarType = readAvatarType(accountUuid)
        val avatarMonogram = readAvatarMonogram(accountUuid)

        if (avatarType == AvatarTypeDto.MONOGRAM.name && avatarMonogram.isEmpty()) {
            val monogram = generateAvatarMonogram(accountUuid)
            insertAvatarMonogram(accountUuid, monogram)
        }
    }

    private fun generateAvatarMonogram(accountUuid: String): String {
        val name = readName(accountUuid)
        val email = readEmail(accountUuid)
        return getAvatarMonogram(name, email)
    }

    private fun getAvatarMonogram(name: String?, email: String?): String {
        return if (name != null && name.isNotEmpty()) {
            composeAvatarMonogram(name)
        } else if (email != null && email.isNotEmpty()) {
            composeAvatarMonogram(email)
        } else {
            AVATAR_MONOGRAM_DEFAULT
        }
    }

    private fun composeAvatarMonogram(name: String): String {
        return name.replace(" ", "").take(2).uppercase()
    }

    private fun readAvatarType(accountUuid: String): String {
        return migrationsHelper.readValue(db, "$accountUuid.$AVATAR_TYPE_KEY") ?: ""
    }

    private fun readAvatarMonogram(accountUuid: String): String {
        return migrationsHelper.readValue(db, "$accountUuid.$AVATAR_MONOGRAM_KEY") ?: ""
    }

    private fun readName(accountUuid: String): String {
        return migrationsHelper.readValue(db, "$accountUuid.$NAME_KEY") ?: ""
    }

    private fun readEmail(accountUuid: String): String {
        return migrationsHelper.readValue(db, "$accountUuid.$EMAIL_KEY") ?: ""
    }

    private fun insertAvatarMonogram(accountUuid: String, monogram: String) {
        migrationsHelper.insertValue(db, "$accountUuid.$AVATAR_MONOGRAM_KEY", monogram)
    }

    private companion object {
        const val NAME_KEY = "name.0"
        const val EMAIL_KEY = "email.0"
        const val AVATAR_TYPE_KEY = "avatarType"
        const val AVATAR_MONOGRAM_KEY = "avatarMonogram"

        private const val AVATAR_MONOGRAM_DEFAULT = "XX"
    }
}
