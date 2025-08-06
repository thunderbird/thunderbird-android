package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

/**
 * Migration to ensure all accounts have an avatar type set.
 * This fixes an issue where migration 27 might not have set the avatar type correctly.
 */
@Suppress("TooManyFunctions")
class StorageMigrationTo28(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun ensureAvatarSet() {
        val accountUuidsValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsValue.split(",")
        for (accountUuid in accountUuids) {
            ensureAvatarTypeForAccount(accountUuid)
        }
    }

    private fun ensureAvatarTypeForAccount(accountUuid: String) {
        var avatarType = readAvatarType(accountUuid)
        val avatarMonogram = readAvatarMonogram(accountUuid)

        if (avatarType.isEmpty()) {
            avatarType = AvatarTypeDto.MONOGRAM.name
            insertAvatarType(accountUuid, avatarType)
        }

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

    private fun insertAvatarType(accountUuid: String, avatarType: String) {
        migrationsHelper.insertValue(db, "$accountUuid.$AVATAR_TYPE_KEY", avatarType)
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
