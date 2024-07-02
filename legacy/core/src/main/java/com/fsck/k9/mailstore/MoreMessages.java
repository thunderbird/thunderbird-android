package com.fsck.k9.mailstore;


public enum MoreMessages {
    UNKNOWN("unknown"),
    FALSE("false"),
    TRUE("true");

    private final String databaseName;

    MoreMessages(String databaseName) {
        this.databaseName = databaseName;
    }

    public static MoreMessages fromDatabaseName(String databaseName) {
        for (MoreMessages value : MoreMessages.values()) {
            if (value.databaseName.equals(databaseName)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown value: " + databaseName);
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
