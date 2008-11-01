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


package org.apache.james.mime4j.util;

import java.io.InputStream;
import java.io.IOException;

public class PositionInputStream extends InputStream {

    private final InputStream inputStream;
    protected long position = 0;
    private long markedPosition = 0;

    public PositionInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public long getPosition() {
        return position;
    }

    public int available() throws IOException {
        return inputStream.available();
    }

    public int read() throws IOException {
        int b = inputStream.read();
        if (b != -1)
            position++;
        return b;
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public void reset() throws IOException {
        inputStream.reset();
        position = markedPosition;
    }

    public boolean markSupported() {
        return inputStream.markSupported();
    }

    public void mark(int readlimit) {
        inputStream.mark(readlimit);
        markedPosition = position;
    }

    public long skip(long n) throws IOException {
        final long c = inputStream.skip(n);
        position += c;
        return c;
    }

    public int read(byte b[]) throws IOException {
        final int c = inputStream.read(b);
        position += c;
        return c;
    }

    public int read(byte b[], int off, int len) throws IOException {
        final int c = inputStream.read(b, off, len);
        position += c;
        return c;
    }

}
