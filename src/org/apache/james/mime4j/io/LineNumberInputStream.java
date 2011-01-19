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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>InputStream</code> used by the parser to wrap the original user
 * supplied stream. This stream keeps track of the current line number.
 */
public class LineNumberInputStream extends FilterInputStream implements
        LineNumberSource {
    private int lineNumber = 1;

    /**
     * Creates a new <code>LineNumberInputStream</code>.
     *
     * @param is
     *            the stream to read from.
     */
    public LineNumberInputStream(InputStream is) {
        super(is);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b == '\n') {
            lineNumber++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        for (int i = off; i < off + n; i++) {
            if (b[i] == '\n') {
                lineNumber++;
            }
        }
        return n;
    }
}
