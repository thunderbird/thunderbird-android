package com.fsck.k9.mail.store.webdav;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Utility methods for WebDAV code.
 */
class WebDavUtils {

    /**
     * Returns a string of the stacktrace for a Throwable to allow for easy inline printing of errors.
     */
    static String processException(Throwable t) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        t.printStackTrace(ps);
        ps.close();

        return baos.toString();
    }
}
