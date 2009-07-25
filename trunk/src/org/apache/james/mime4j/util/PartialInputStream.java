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

public class PartialInputStream extends PositionInputStream {
    private final long limit;

    public PartialInputStream(InputStream inputStream, long offset, long length) throws IOException {
        super(inputStream);
        inputStream.skip(offset);
        this.limit = offset + length;
    }

    public int available() throws IOException {
        return Math.min(super.available(), getBytesLeft());
    }

    public int read() throws IOException {
        if (limit > position)
            return super.read();
        else
            return -1;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        len = Math.min(len, getBytesLeft());
        return super.read(b, off, len);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public long skip(long n) throws IOException {
        n = Math.min(n, getBytesLeft());
        return super.skip(n);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private int getBytesLeft() {
        return (int)Math.min(Integer.MAX_VALUE, limit - position);
    }
}
