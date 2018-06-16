package com.fsck.k9.helper;


import com.fsck.k9.mail.MessagingException;


public class ExceptionHelper {
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
        }

        // Remove the namespace on the exception so we have a fighting chance of seeing more of the error in the
        // notification.
        String simpleName = rootCause.getClass().getSimpleName();
        return (rootCause.getLocalizedMessage() != null) ?
                simpleName + ": " + rootCause.getLocalizedMessage() : simpleName;
    }
}
