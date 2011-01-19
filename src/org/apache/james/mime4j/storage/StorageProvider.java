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
 * Provides a strategy for storing the contents of an <code>InputStream</code>
 * or retrieving the content written to an <code>OutputStream</code>.
 */
public interface StorageProvider {
    /**
     * Stores the contents of the given <code>InputStream</code>.
     *
     * @param in stream containing the data to store.
     * @return a {@link Storage} instance that can be used to retrieve the
     *         stored content.
     * @throws IOException if an I/O error occurs.
     */
    Storage store(InputStream in) throws IOException;

    /**
     * Creates a {@link StorageOutputStream} where data to be stored can be
     * written to. Subsequently the user can call
     * {@link StorageOutputStream#toStorage() toStorage()} on that object to get
     * a {@link Storage} instance that holds the data that has been written.
     *
     * @return a {@link StorageOutputStream} where data can be written to.
     * @throws IOException
     *             if an I/O error occurs.
     */
    StorageOutputStream createStorageOutputStream() throws IOException;
}
