package com.fsck.k9.mailstore;


import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType;
import org.jetbrains.annotations.NotNull;


public enum DatabasePreviewType {
    NONE("none", PreviewType.NONE),
    TEXT("text", PreviewType.TEXT),
    ENCRYPTED("encrypted", PreviewType.ENCRYPTED),
    ERROR("error", PreviewType.ERROR);


    private final String databaseValue;
    private final PreviewType previewType;


    DatabasePreviewType(String databaseValue, PreviewType previewType) {
        this.databaseValue = databaseValue;
        this.previewType = previewType;
    }

    @NotNull
    public static DatabasePreviewType fromDatabaseValue(String databaseValue) {
        for (DatabasePreviewType databasePreviewType : values()) {
            if (databasePreviewType.getDatabaseValue().equals(databaseValue)) {
                return databasePreviewType;
            }
        }

        throw new AssertionError("Unknown database value: " + databaseValue);
    }

    public static DatabasePreviewType fromPreviewType(PreviewType previewType) {
        for (DatabasePreviewType databasePreviewType : values()) {
            if (databasePreviewType.previewType == previewType) {
                return databasePreviewType;
            }
        }

        throw new AssertionError("Unknown preview type: " + previewType);
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public PreviewType getPreviewType() {
        return previewType;
    }
}
