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

package org.apache.james.mime4j.io;

import org.apache.james.mime4j.util.ByteArrayBuffer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream capable of reading lines of text.
 */
public abstract class LineReaderInputStream extends FilterInputStream {

    protected LineReaderInputStream(InputStream in) {
        super(in);
    }

    /**
     * Reads one line of text into the given {@link ByteArrayBuffer}.
     *
     * @param dst Destination
     * @return number of bytes copied or <code>-1</code> if the end of
     * the stream has been reached.
     *
     * @throws MaxLineLimitException if the line exceeds a limit on
     *   the line length imposed by a subclass.
     * @throws IOException in case of an I/O error.
     */
    public abstract int readLine(final ByteArrayBuffer dst)
            throws MaxLineLimitException, IOException;

    /**
     * Tries to unread the last read line.
     *
     * Implementation may refuse to unread a new buffer until the previous
     * unread one has been competely consumed.
     *
     * Implementations will directly use the byte array backed by buf, so
     * make sure to not alter it anymore once this method has been called.
     *
     * @return true if the unread has been succesfull.
     */
    public abstract boolean unread(ByteArrayBuffer buf);

}
