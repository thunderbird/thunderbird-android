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

/**
 * An immutable sequence of bytes.
 */
public interface ByteSequence {

    /**
     * An empty byte sequence.
     */
    ByteSequence EMPTY = new EmptyByteSequence();

    /**
     * Returns the length of this byte sequence.
     *
     * @return the number of <code>byte</code>s in this sequence.
     */
    int length();

    /**
     * Returns the <code>byte</code> value at the specified index.
     *
     * @param index
     *            the index of the <code>byte</code> value to be returned.
     * @return the corresponding <code>byte</code> value
     * @throws IndexOutOfBoundsException
     *             if <code>index < 0 || index >= length()</code>.
     */
    byte byteAt(int index);

    /**
     * Copies the contents of this byte sequence into a newly allocated byte
     * array and returns that array.
     *
     * @return a byte array holding a copy of this byte sequence.
     */
    byte[] toByteArray();

}
