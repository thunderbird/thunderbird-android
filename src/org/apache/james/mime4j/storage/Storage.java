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
 * Can be used to read data that has been stored by a {@link StorageProvider}.
 */
public interface Storage {
    /**
     * Returns an <code>InputStream</code> that can be used to read the stored
     * data. The input stream should be closed by the caller when it is no
     * longer needed.
     * <p>
     * Note: The stream should NOT be wrapped in a
     * <code>BufferedInputStream</code> by the caller. If the implementing
     * <code>Storage</code> creates a stream which would benefit from being
     * buffered it is the <code>Storage</code>'s responsibility to wrap it.
     *
     * @return an <code>InputStream</code> for reading the stored data.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws IllegalStateException
     *             if this <code>Storage</code> instance has been deleted.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Deletes the data held by this <code>Storage</code> as soon as possible.
     * Deleting an already deleted <code>Storage</code> has no effect.
     */
    void delete();

}
