package com.fsck.k9.preferences;

public class SettingsImportExportException extends Exception {
    private static final long serialVersionUID = -6042736634079588513L;

    public SettingsImportExportException() {
        super();
    }

    public SettingsImportExportException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public SettingsImportExportException(String detailMessage) {
        super(detailMessage);
    }

    public SettingsImportExportException(Throwable throwable) {
        super(throwable);
    }

}
