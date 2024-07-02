package com.fsck.k9.mailstore

enum class SendState(val databaseName: String) {
    READY("ready"),
    RETRIES_EXCEEDED("retries_exceeded"),
    ERROR("error"),
    ;

    companion object {
        @JvmStatic
        fun fromDatabaseName(databaseName: String): SendState {
            return entries.firstOrNull { it.databaseName == databaseName }
                ?: throw IllegalArgumentException("Unknown value: $databaseName")
        }
    }
}
