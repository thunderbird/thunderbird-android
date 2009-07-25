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

package org.apache.james.mime4j.decoder;

import java.util.Iterator;

public class ByteQueue {

    private UnboundedFifoByteBuffer buf;
    private int initialCapacity = -1;

    public ByteQueue() {
        buf = new UnboundedFifoByteBuffer();
    }

    public ByteQueue(int initialCapacity) {
        buf = new UnboundedFifoByteBuffer(initialCapacity);
        this.initialCapacity = initialCapacity;
    }

    public void enqueue(byte b) {
        buf.add(b);
    }

    public byte dequeue() {
        return buf.remove();
    }

    public int count() {
        return buf.size();
    }

    public void clear() {
        if (initialCapacity != -1)
            buf = new UnboundedFifoByteBuffer(initialCapacity);
        else
            buf = new UnboundedFifoByteBuffer();
    }

    public Iterator iterator() {
        return buf.iterator();
    }


}
