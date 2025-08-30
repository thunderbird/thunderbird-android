package com.fsck.k9.mailstore

import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType

enum class DatabasePreviewType(
    @JvmField val databaseValue: String,
    @JvmField val previewType: PreviewType,
) {
    NONE("none", PreviewType.NONE),
    TEXT("text", PreviewType.TEXT),
    ENCRYPTED("encrypted", PreviewType.ENCRYPTED),
    ERROR("error", PreviewType.ERROR),
    ;

    companion object {
        @JvmStatic
        fun fromDatabaseValue(databaseValue: String): DatabasePreviewType {
            return entries.find {
                it.databaseValue == databaseValue
            } ?: throw AssertionError("Unknown database value: $databaseValue")
        }

        @JvmStatic
        fun fromPreviewType(previewType: PreviewType): DatabasePreviewType {
            return entries.find {
                it.previewType == previewType
            } ?: throw AssertionError("Unknown preview type: $previewType")
        }
    }
}
