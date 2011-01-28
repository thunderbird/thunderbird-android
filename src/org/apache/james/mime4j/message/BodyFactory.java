/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.message;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Disposable;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.storage.DefaultStorageProvider;
import org.apache.james.mime4j.storage.MultiReferenceStorage;
import org.apache.james.mime4j.storage.Storage;
import org.apache.james.mime4j.storage.StorageProvider;
import org.apache.james.mime4j.util.CharsetUtil;

/**
 * Factory for creating message bodies.
 */
public class BodyFactory {

    private static final Charset FALLBACK_CHARSET = CharsetUtil.DEFAULT_CHARSET;

    private final StorageProvider storageProvider;
    private final DecodeMonitor monitor;

    /**
     * Creates a new <code>BodyFactory</code> instance that uses the default
     * storage provider for creating message bodies from input streams.
     */
    public BodyFactory() {
        this(null, null);
    }

    /**
     * Creates a new <code>BodyFactory</code> instance that uses the given
     * storage provider for creating message bodies from input streams.
     *
     * @param storageProvider
     *            a storage provider or <code>null</code> to use the default
     *            one.
     */
    public BodyFactory(
            final StorageProvider storageProvider,
            final DecodeMonitor monitor) {
        this.storageProvider =
            storageProvider != null ? storageProvider : DefaultStorageProvider.getInstance();
        this.monitor =
            monitor != null ? monitor : DecodeMonitor.SILENT;
    }

    /**
     * Returns the <code>StorageProvider</code> this <code>BodyFactory</code>
     * uses to create message bodies from input streams.
     *
     * @return a <code>StorageProvider</code>.
     */
    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    /**
     * Creates a {@link BinaryBody} that holds the content of the given input
     * stream.
     *
     * @param is
     *            input stream to create a message body from.
     * @return a binary body.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public BinaryBody binaryBody(InputStream is) throws IOException {
        if (is == null)
            throw new IllegalArgumentException();

        Storage storage = storageProvider.store(is);
        return new StorageBinaryBody(new MultiReferenceStorage(storage));
    }

    /**
     * Creates a {@link BinaryBody} that holds the content of the given
     * {@link Storage}.
     * <p>
     * Note that the caller must not invoke {@link Storage#delete() delete()} on
     * the given <code>Storage</code> object after it has been passed to this
     * method. Instead the message body created by this method takes care of
     * deleting the storage when it gets disposed of (see
     * {@link Disposable#dispose()}).
     *
     * @param storage
     *            storage to create a message body from.
     * @return a binary body.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public BinaryBody binaryBody(Storage storage) throws IOException {
        if (storage == null)
            throw new IllegalArgumentException();

        return new StorageBinaryBody(new MultiReferenceStorage(storage));
    }

    /**
     * Creates a {@link TextBody} that holds the content of the given input
     * stream.
     * <p>
     * &quot;us-ascii&quot; is used to decode the byte content of the
     * <code>Storage</code> into a character stream when calling
     * {@link TextBody#getReader() getReader()} on the returned object.
     *
     * @param is
     *            input stream to create a message body from.
     * @return a text body.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public TextBody textBody(InputStream is) throws IOException {
        if (is == null)
            throw new IllegalArgumentException();

        Storage storage = storageProvider.store(is);
        return new StorageTextBody(new MultiReferenceStorage(storage),
                CharsetUtil.DEFAULT_CHARSET);
    }

    /**
     * Creates a {@link TextBody} that holds the content of the given input
     * stream.
     * <p>
     * The charset corresponding to the given MIME charset name is used to
     * decode the byte content of the input stream into a character stream when
     * calling {@link TextBody#getReader() getReader()} on the returned object.
     * If the MIME charset has no corresponding Java charset or the Java charset
     * cannot be used for decoding then &quot;us-ascii&quot; is used instead.
     *
     * @param is
     *            input stream to create a message body from.
     * @param mimeCharset
     *            name of a MIME charset.
     * @return a text body.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public TextBody textBody(InputStream is, String mimeCharset)
            throws IOException {
        if (is == null)
            throw new IllegalArgumentException();
        if (mimeCharset == null)
            throw new IllegalArgumentException();

        Storage storage = storageProvider.store(is);
        Charset charset = toJavaCharset(mimeCharset, false, monitor);
        return new StorageTextBody(new MultiReferenceStorage(storage), charset);
    }

    /**
     * Creates a {@link TextBody} that holds the content of the given
     * {@link Storage}.
     * <p>
     * &quot;us-ascii&quot; is used to decode the byte content of the
     * <code>Storage</code> into a character stream when calling
     * {@link TextBody#getReader() getReader()} on the returned object.
     * <p>
     * Note that the caller must not invoke {@link Storage#delete() delete()} on
     * the given <code>Storage</code> object after it has been passed to this
     * method. Instead the message body created by this method takes care of
     * deleting the storage when it gets disposed of (see
     * {@link Disposable#dispose()}).
     *
     * @param storage
     *            storage to create a message body from.
     * @return a text body.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public TextBody textBody(Storage storage) throws IOException {
        if (storage == null)
            throw new IllegalArgumentException();

        return new StorageTextBody(new MultiReferenceStorage(storage),
                CharsetUtil.DEFAULT_CHARSET);
    }

    /**
     * Creates a {@link TextBody} that holds the content of the given
     * {@link Storage}.
     * <p>
     * The charset corresponding to the given MIME charset name is used to
     * decode the byte content of the <code>Storage</code> into a character
     * stream when calling {@link TextBody#getReader() getReader()} on the
     * returned object. If the MIME charset has no corresponding Java charset or
     * the Java charset cannot be used for decoding then &quot;us-ascii&quot; is
     * used instead.
     * <p>
     * Note that the caller must not invoke {@link Storage#delete() delete()} on
     * the given <code>Storage</code> object after it has been passed to this
     * method. Instead the message body created by this method takes care of
     * deleting the storage when it gets disposed of (see
     * {@link Disposable#dispose()}).
     *
     * @param storage
     *            storage to create a message body from.
     * @param mimeCharset
     *            name of a MIME charset.
     * @return a text body.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public TextBody textBody(Storage storage, String mimeCharset)
            throws IOException {
        if (storage == null)
            throw new IllegalArgumentException();
        if (mimeCharset == null)
            throw new IllegalArgumentException();

        Charset charset = toJavaCharset(mimeCharset, false, monitor);
        return new StorageTextBody(new MultiReferenceStorage(storage), charset);
    }

    /**
     * Creates a {@link TextBody} that holds the content of the given string.
     * <p>
     * &quot;us-ascii&quot; is used to encode the characters of the string into
     * a byte stream when calling
     * {@link SingleBody#writeTo(java.io.OutputStream) writeTo(OutputStream)} on
     * the returned object.
     *
     * @param text
     *            text to create a message body from.
     * @return a text body.
     */
    public TextBody textBody(String text) {
        if (text == null)
            throw new IllegalArgumentException();

        return new StringTextBody(text, CharsetUtil.DEFAULT_CHARSET);
    }

    /**
     * Creates a {@link TextBody} that holds the content of the given string.
     * <p>
     * The charset corresponding to the given MIME charset name is used to
     * encode the characters of the string into a byte stream when calling
     * {@link SingleBody#writeTo(java.io.OutputStream) writeTo(OutputStream)} on
     * the returned object. If the MIME charset has no corresponding Java
     * charset or the Java charset cannot be used for encoding then
     * &quot;us-ascii&quot; is used instead.
     *
     * @param text
     *            text to create a message body from.
     * @param mimeCharset
     *            name of a MIME charset.
     * @return a text body.
     */
    public TextBody textBody(String text, String mimeCharset) {
        if (text == null)
            throw new IllegalArgumentException();
        if (mimeCharset == null)
            throw new IllegalArgumentException();

        Charset charset = toJavaCharset(mimeCharset, true, monitor);
        return new StringTextBody(text, charset);
    }

    private static Charset toJavaCharset(
            final String mimeCharset,
            boolean forEncoding,
            final DecodeMonitor monitor) {
        String charset = CharsetUtil.toJavaCharset(mimeCharset);
        if (charset == null) {
            if (monitor.isListening()) {
                monitor.warn(
                        "MIME charset '" + mimeCharset + "' has no "
                        + "corresponding Java charset", "Using "
                        + FALLBACK_CHARSET + " instead.");
            }
            return FALLBACK_CHARSET;
        }

        if (forEncoding && !CharsetUtil.isEncodingSupported(charset)) {
            if (monitor.isListening()) {
                monitor.warn(
                        "MIME charset '" + mimeCharset
                        + "' does not support encoding", "Using "
                        + FALLBACK_CHARSET + " instead.");
            }
            return FALLBACK_CHARSET;
        }

        if (!forEncoding && !CharsetUtil.isDecodingSupported(charset)) {
            if (monitor.isListening()) {
                monitor.warn(
                        "MIME charset '" + mimeCharset
                        + "' does not support decoding", "Using "
                        + FALLBACK_CHARSET + " instead.");
            }
            return FALLBACK_CHARSET;
        }

        return Charset.forName(charset);
    }

}
