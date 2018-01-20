package com.fsck.k9.controller;


import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;


public class ControllerUtils {
    public static void closeFolder(Folder f) {
        if (f != null) {
            f.close();
        }
    }

    public static String getRootCauseMessage(Throwable t) {
        Throwable rootCause = t;
        Throwable nextCause;
        do {
            nextCause = rootCause.getCause();
            if (nextCause != null) {
                rootCause = nextCause;
            }
        } while (nextCause != null);
        if (rootCause instanceof MessagingException) {
            return rootCause.getMessage();
        } else {
            // Remove the namespace on the exception so we have a fighting chance of seeing more of the error in the
            // notification.
            return (rootCause.getLocalizedMessage() != null)
                    ? (rootCause.getClass().getSimpleName() + ": " + rootCause.getLocalizedMessage())
                    : rootCause.getClass().getSimpleName();
        }
    }
}
