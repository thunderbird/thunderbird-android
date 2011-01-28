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

import org.apache.james.mime4j.codec.CodecUtil;

/**
 * Abstract implementation of {@link StorageProvider} that implements
 * {@link StorageProvider#store(InputStream) store(InputStream)} by copying the
 * input stream to a {@link StorageOutputStream} obtained from
 * {@link StorageProvider#createStorageOutputStream() createStorageOutputStream()}.
 */
public abstract class AbstractStorageProvider implements StorageProvider {

    /**
     * Sole constructor.
     */
    protected AbstractStorageProvider() {
    }

    /**
     * This implementation creates a {@link StorageOutputStream} by calling
     * {@link StorageProvider#createStorageOutputStream() createStorageOutputStream()}
     * and copies the content of the given input stream to that output stream.
     * It then calls {@link StorageOutputStream#toStorage()} on the output
     * stream and returns this object.
     *
     * @param in
     *            stream containing the data to store.
     * @return a {@link Storage} instance that can be used to retrieve the
     *         stored content.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public final Storage store(InputStream in) throws IOException {
        StorageOutputStream out = createStorageOutputStream();
        CodecUtil.copy(in, out);
        return out.toStorage();
    }

}
