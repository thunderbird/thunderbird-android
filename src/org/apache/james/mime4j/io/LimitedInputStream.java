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

import java.io.InputStream;
import java.io.IOException;

public class LimitedInputStream extends PositionInputStream {

    private final long limit;

    public LimitedInputStream(InputStream instream, long limit) {
        super(instream);
        if (limit < 0) {
            throw new IllegalArgumentException("Limit may not be negative");
        }
        this.limit = limit;
    }

    private void enforceLimit() throws IOException {
        if (position >= limit) {
            throw new IOException("Input stream limit exceeded");
        }
    }

    @Override
    public int read() throws IOException {
        enforceLimit();
        return super.read();
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        enforceLimit();
        len = Math.min(len, getBytesLeft());
        return super.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        enforceLimit();
        n = Math.min(n, getBytesLeft());
        return super.skip(n);
    }

    private int getBytesLeft() {
        return (int)Math.min(Integer.MAX_VALUE, limit - position);
    }

}
