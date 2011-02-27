package com.fsck.k9.preferences;

public class StorageImportExportException extends Exception {

    public StorageImportExportException() {
        super();
    }

    public StorageImportExportException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public StorageImportExportException(String detailMessage) {
        super(detailMessage);
    }

    public StorageImportExportException(Throwable throwable) {
        super(throwable);
    }

}
