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
import java.io.InputStream;
import java.io.IOException;

public class PositionInputStream extends FilterInputStream {

    protected long position = 0;
    private long markedPosition = 0;

    public PositionInputStream(InputStream inputStream) {
        super(inputStream);
    }

    public long getPosition() {
        return position;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1)
            position++;
        return b;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void reset() throws IOException {
        in.reset();
        position = markedPosition;
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
        markedPosition = position;
    }

    @Override
    public long skip(long n) throws IOException {
        final long c = in.skip(n);
        if (c > 0)
            position += c;
        return c;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        final int c = in.read(b, off, len);
        if (c > 0)
            position += c;
        return c;
    }

}
