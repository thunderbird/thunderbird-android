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
import java.util.NoSuchElementException;

/**
 * UnboundedFifoByteBuffer is a very efficient buffer implementation.
 * According to performance testing, it exhibits a constant access time, but it
 * also outperforms ArrayList when used for the same purpose.
 * <p>
 * The removal order of an <code>UnboundedFifoByteBuffer</code> is based on the insertion
 * order; elements are removed in the same order in which they were added.
 * The iteration order is the same as the removal order.
 * <p>
 * The {@link #remove()} and {@link #get()} operations perform in constant time.
 * The {@link #add(Object)} operation performs in amortized constant time.  All
 * other operations perform in linear time or worse.
 * <p>
 * Note that this implementation is not synchronized.  The following can be
 * used to provide synchronized access to your <code>UnboundedFifoByteBuffer</code>:
 * <pre>
 *   Buffer fifo = BufferUtils.synchronizedBuffer(new UnboundedFifoByteBuffer());
 * </pre>
 * <p>
 * This buffer prevents null objects from being added.
 *
 * @since Commons Collections 3.0 (previously in main package v2.1)
 * @version $Revision: 1.1 $ $Date: 2004/08/24 06:52:02 $
 *
 * 
 * 
 * 
 * 
 * 
 */
class UnboundedFifoByteBuffer {

    protected byte[] buffer;
    protected int head;
    protected int tail;

    /**
     * Constructs an UnboundedFifoByteBuffer with the default number of elements.
     * It is exactly the same as performing the following:
     *
     * <pre>
     *   new UnboundedFifoByteBuffer(32);
     * </pre>
     */
    public UnboundedFifoByteBuffer() {
        this(32);
    }

    /**
     * Constructs an UnboundedFifoByteBuffer with the specified number of elements.
     * The integer must be a positive integer.
     *
     * @param initialSize  the initial size of the buffer
     * @throws IllegalArgumentException  if the size is less than 1
     */
    public UnboundedFifoByteBuffer(int initialSize) {
        if (initialSize <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        buffer = new byte[initialSize + 1];
        head = 0;
        tail = 0;
    }

    /**
     * Returns the number of elements stored in the buffer.
     *
     * @return this buffer's size
     */
    public int size() {
        int size = 0;

        if (tail < head) {
            size = buffer.length - head + tail;
        } else {
            size = tail - head;
        }

        return size;
    }

    /**
     * Returns true if this buffer is empty; false otherwise.
     *
     * @return true if this buffer is empty
     */
    public boolean isEmpty() {
        return (size() == 0);
    }

    /**
     * Adds the given element to this buffer.
     *
     * @param b  the byte to add
     * @return true, always
     */
    public boolean add(final byte b) {

        if (size() + 1 >= buffer.length) {
            byte[] tmp = new byte[((buffer.length - 1) * 2) + 1];

            int j = 0;
            for (int i = head; i != tail;) {
                tmp[j] = buffer[i];
                buffer[i] = 0;

                j++;
                i++;
                if (i == buffer.length) {
                    i = 0;
                }
            }

            buffer = tmp;
            head = 0;
            tail = j;
        }

        buffer[tail] = b;
        tail++;
        if (tail >= buffer.length) {
            tail = 0;
        }
        return true;
    }

    /**
     * Returns the next object in the buffer.
     *
     * @return the next object in the buffer
     * @throws BufferUnderflowException  if this buffer is empty
     */
    public byte get() {
        if (isEmpty()) {
            throw new IllegalStateException("The buffer is already empty");
        }

        return buffer[head];
    }

    /**
     * Removes the next object from the buffer
     *
     * @return the removed object
     * @throws BufferUnderflowException  if this buffer is empty
     */
    public byte remove() {
        if (isEmpty()) {
            throw new IllegalStateException("The buffer is already empty");
        }

        byte element = buffer[head];

        head++;
        if (head >= buffer.length) {
            head = 0;
        }

        return element;
    }

    /**
     * Increments the internal index.
     *
     * @param index  the index to increment
     * @return the updated index
     */
    private int increment(int index) {
        index++;
        if (index >= buffer.length) {
            index = 0;
        }
        return index;
    }

    /**
     * Decrements the internal index.
     *
     * @param index  the index to decrement
     * @return the updated index
     */
    private int decrement(int index) {
        index--;
        if (index < 0) {
            index = buffer.length - 1;
        }
        return index;
    }

    /**
     * Returns an iterator over this buffer's elements.
     *
     * @return an iterator over this buffer's elements
     */
    public Iterator iterator() {
        return new Iterator() {

            private int index = head;
            private int lastReturnedIndex = -1;

            public boolean hasNext() {
                return index != tail;

            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                lastReturnedIndex = index;
                index = increment(index);
                return new Byte(buffer[lastReturnedIndex]);
            }

            public void remove() {
                if (lastReturnedIndex == -1) {
                    throw new IllegalStateException();
                }

                // First element can be removed quickly
                if (lastReturnedIndex == head) {
                    UnboundedFifoByteBuffer.this.remove();
                    lastReturnedIndex = -1;
                    return;
                }

                // Other elements require us to shift the subsequent elements
                int i = lastReturnedIndex + 1;
                while (i != tail) {
                    if (i >= buffer.length) {
                        buffer[i - 1] = buffer[0];
                        i = 0;
                    } else {
                        buffer[i - 1] = buffer[i];
                        i++;
                    }
                }

                lastReturnedIndex = -1;
                tail = decrement(tail);
                buffer[tail] = 0;
                index = decrement(index);
            }

        };
    }

}