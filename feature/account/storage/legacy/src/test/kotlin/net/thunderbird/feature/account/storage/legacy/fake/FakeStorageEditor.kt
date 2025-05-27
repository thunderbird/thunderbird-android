package net.thunderbird.feature.account.storage.legacy.fake

import net.thunderbird.core.preference.storage.StorageEditor

class FakeStorageEditor : StorageEditor {
    val values = mutableMapOf<String, String?>()

    val removedKeys = mutableListOf<String>()

    override fun putBoolean(key: String, value: Boolean): StorageEditor {
        values[key] = value.toString()
        return this
    }

    override fun putInt(key: String, value: Int): StorageEditor {
        values[key] = value.toString()
        return this
    }

    override fun putLong(key: String, value: Long): StorageEditor {
        values[key] = value.toString()
        return this
    }

    override fun putString(key: String, value: String?): StorageEditor {
        values[key] = value
        return this
    }

    override fun remove(key: String): StorageEditor {
        values.remove(key)
        removedKeys.add(key)
        return this
    }

    override fun commit(): Boolean = true
}
