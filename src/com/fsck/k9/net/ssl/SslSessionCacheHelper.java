package com.fsck.k9.net.ssl;

import java.io.File;
import java.lang.reflect.Method;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;

import com.fsck.k9.K9;

import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * A class to help with associating an {@code SSLContext} with a persistent
 * file-based cache of SSL sessions.
 * <p>
 * This uses reflection to achieve its task.
 * <p>
 * The alternative to this would be to use {@link SSLCertificateSocketFactory}
 * which also provides session caching. The problem with using that occurs when
 * using STARTTLS in combination with
 * {@code TrustedSocketFactory.hardenSocket(SSLSocket)}. The result is that
 * {@code hardenSocket()} fails to change anything because by the time it is
 * applied to the socket, the SSL handshake has already been completed. (This is
 * because of another feature of {@link SSLCertificateSocketFactory} whereby it
 * performs host name verification which necessitates initiating the SSL
 * handshake immediately on socket creation.)
 * <p>
 * If eventually the use of hardenSocket() should become unnecessary, then
 * switching to using {@link SSLCertificateSocketFactory} would be a better
 * solution.
 */
public class SslSessionCacheHelper {
    private static Object sSessionCache;
    private static Method sSetPersistentCacheMethod;
    private static boolean sIsDisabled = false;

    static {
        final String packageName;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            packageName = "org.apache.harmony.xnet.provider.jsse";
        } else {
            packageName = "com.android.org.conscrypt";
        }
        final File cacheDirectory = K9.app.getDir("sslcache", Context.MODE_PRIVATE);
        try {
            Class<?> fileClientSessionCacheClass = Class.forName(packageName +
                    ".FileClientSessionCache");
            Method usingDirectoryMethod = fileClientSessionCacheClass
                    .getMethod("usingDirectory", File.class);
            sSessionCache = usingDirectoryMethod.invoke(null, cacheDirectory);

            Class<?> sslClientSessionCacheClass = Class.forName(packageName +
                    ".SSLClientSessionCache");
            Class<?> clientSessionContextClass = Class.forName(packageName +
                    ".ClientSessionContext");
            sSetPersistentCacheMethod = clientSessionContextClass.getMethod(
                    "setPersistentCache", sslClientSessionCacheClass);
        } catch (Exception e) {
            // Something went wrong. Proceed without a session cache.
            Log.e(K9.LOG_TAG, "Failed to initialize SslSessionCacheHelper: " + e);
            sIsDisabled = true;
        }
    }

    /**
     * Associate an {@code SSLContext} with a persistent file-based cache of SSL
     * sessions which can be used when re-establishing a connection to the same
     * server.
     * <p>
     * This is beneficial because it can eliminate redundant cryptographic
     * computations and network traffic, thus saving time and conserving power.
     */
    public static void setPersistentCache(SSLContext sslContext) {
        if (sIsDisabled) {
            return;
        }
        try {
            SSLSessionContext sessionContext = sslContext.getClientSessionContext();
            sSetPersistentCacheMethod.invoke(sessionContext, sSessionCache);
        } catch (Exception e) {
            // Something went wrong. Proceed without a session cache.
            Log.e(K9.LOG_TAG, "Failed to initialize persistent SSL cache: " + e);
            sIsDisabled = true;
        }
    }
}
