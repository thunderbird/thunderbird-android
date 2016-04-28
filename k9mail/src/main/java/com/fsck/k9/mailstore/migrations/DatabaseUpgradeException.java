
package com.fsck.k9.mailstore.migrations;

public class DatabaseUpgradeException extends Exception {
    public static final long serialVersionUID = -1;

    public DatabaseUpgradeException(String message) {
        super(message);
    }

    public DatabaseUpgradeException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
