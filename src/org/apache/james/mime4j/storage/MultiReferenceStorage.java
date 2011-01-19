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

package org.apache.james.mime4j.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * A wrapper around another {@link Storage} that also maintains a reference
 * counter. The inner storage gets deleted only if the reference counter reaches
 * zero.
 * </p>
 * <p>
 * Reference counting is used to delete the storage when it is no longer needed.
 * So, any users of this class should note:
 * </p>
 * <ul>
 * <li>The reference count is set up one on construction. In all other cases,
 * {@link #addReference()} should be called when the storage is shared.</li>
 * <li>The caller of {@link #addReference()} should ensure that
 * {@link #delete()} is called once and only once.</li>
 * <li>Sharing the {@link Storage} instance passed into
 * {@link #MultiReferenceStorage(Storage)} may lead to miscounting and premature
 * deletion</li>
 * </ul>
 */
public class MultiReferenceStorage implements Storage {

    private final Storage storage;
    private int referenceCounter;

    /**
     * Creates a new <code>MultiReferenceStorage</code> instance for the given
     * back-end. The reference counter is initially set to one so the caller
     * does not have to call {@link #addReference()} after this constructor.
     *
     * @param storage
     *            storage back-end that should be reference counted.
     * @throws IllegalArgumentException
     *             when storage is null
     */
    public MultiReferenceStorage(Storage storage) {
        if (storage == null)
            throw new IllegalArgumentException();

        this.storage = storage;
        this.referenceCounter = 1; // caller holds first reference
    }

    /**
     * Increments the reference counter.
     *
     * @throws IllegalStateException
     *             if the reference counter is zero which implies that the
     *             backing storage has already been deleted.
     */
    public void addReference() {
        incrementCounter();
    }

    /**
     * Decrements the reference counter and deletes the inner
     * <code>Storage</code> object if the reference counter reaches zero.
     * <p>
     * A client that holds a reference to this object must make sure not to
     * invoke this method a second time.
     *
     * @throws IllegalStateException
     *             if the reference counter is zero which implies that the
     *             backing storage has already been deleted.
     */
    public void delete() {
        if (decrementCounter()) {
            storage.delete();
        }
    }

    /**
     * Returns the input stream of the inner <code>Storage</code> object.
     *
     * @return an input stream.
     */
    public InputStream getInputStream() throws IOException {
        return storage.getInputStream();
    }

    /**
     * Synchronized increment of reference count.
     *
     * @throws IllegalStateException
     *             when counter is already zero
     */
    private synchronized void incrementCounter() {
        if (referenceCounter == 0)
            throw new IllegalStateException("storage has been deleted");

        referenceCounter++;
    }

    /**
     * Synchronized decrement of reference count.
     *
     * @return true when counter has reached zero, false otherwise
     * @throws IllegalStateException
     *             when counter is already zero
     */
    private synchronized boolean decrementCounter() {
        if (referenceCounter == 0)
            throw new IllegalStateException("storage has been deleted");

        return --referenceCounter == 0;
    }
}
