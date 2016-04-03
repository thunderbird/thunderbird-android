/*
 * Copyright (C) 2016 cketti
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cketti.safecontentresolver;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;


/**
 * A replacement for {@code ContentResolver} that protects against the <em>Surreptitious Sharing</em> attack when
 * opening {@code file://} URIs.
 *
 * <p>
 * When performing a Surreptitious Sharing attack a malicious app tries to trick the user into sending a file private
 * to the target app to an attacker.
 * </p><p>
 * Example:<br>
 * An {@link Intent#ACTION_SEND} intent that contains the attacker's email address as recipient and a {@code file://}
 * URI pointing to the file the email client uses to store account passwords. The attacking app itself doesn't have
 * access to this file because Android's security model prevents that. However, the email client has no trouble
 * opening a file that belongs to itself. If the user believes this to be a legitimate email with a legitimate
 * attachment, e.g. because the malicious app asked them to send a bug report, they will send the email and thereby
 * unknowingly expose their email account passwords to the attacker.
 * </p><p>
 * If your app is a receiver of such {@code Intents} and you want to protect against this attack, replace all
 * occurrences of {@link ContentResolver#openInputStream(Uri)} with {@link SafeContentResolver#openInputStream(Uri)}
 * from this class. {@code SafeContentResolver} will refuse to open {@code file://} URIs pointing to files belonging to
 * this app.
 * </p><p>
 * On older Android versions apps have the ability to create <a href="https://en.wikipedia.org/wiki/Hard_link">hard
 * links</a> to files they don't have read or write access to. That means we can't simply check the absolute file path
 * to learn if a file is stored in our own app-private directory. Instead, we use the {@code fstat} system call to
 * retrieve the User ID the file belongs to. And if it's identical to the UID of this process, access is denied.
 * </p>
 */
public abstract class SafeContentResolver {
    private final ContentResolver contentResolver;


    /**
     * Create a {@link SafeContentResolver} instance.
     *
     * @param context
     *         {@link Context} used to retrieve a {@link ContentResolver} instance.
     */
    public static SafeContentResolver newInstance(Context context) {
        if (context == null) {
            throw new NullPointerException("Argument 'context' must not be null.");
        }

        ContentResolver contentResolver = context.getContentResolver();
        return new SafeContentResolverApi21(contentResolver);
    }

    protected SafeContentResolver(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    /**
     * Open a stream to the content associated with a URI.
     *
     * <p>
     * If the provided URI is not a {@code file://} URI, {@link ContentResolver#openInputStream(Uri)} is used to open a
     * stream. If it is a {@code file://}, this method makes sure the file isn't owned by this app.
     * </p>
     *
     * @param uri
     *         The URI pointing to the content to access.
     *
     * @return {@code InputStream} to access the content.
     *
     * @throws FileNotFoundException
     *         If the provided URI could not be opened or if it points to a file owned by this app.
     */
    public InputStream openInputStream(Uri uri) throws FileNotFoundException {
        if (uri == null) {
            throw new NullPointerException("Argument 'uri' must not be null");
        }

        String scheme = uri.getScheme();
        if (!ContentResolver.SCHEME_FILE.equals(scheme)) {
            return contentResolver.openInputStream(uri);
        }

        File file = new File(uri.getPath());
        ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        int fileUid = getFileUidOrThrow(fileDescriptor);
        if (fileUid == android.os.Process.myUid()) {
            throw new FileNotFoundException("File is owned by the application itself");
        }

        AssetFileDescriptor fd = new AssetFileDescriptor(parcelFileDescriptor, 0, -1);
        try {
            return fd.createInputStream();
        } catch (IOException e) {
            throw new FileNotFoundException("Unable to create stream");
        }
    }

    protected abstract int getFileUidOrThrow(FileDescriptor fileDescriptor) throws FileNotFoundException;
}
