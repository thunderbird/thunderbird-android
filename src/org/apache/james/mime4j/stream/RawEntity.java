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

package org.apache.james.mime4j.stream;


import java.io.InputStream;

/**
 * Raw MIME entity. Such entities will not be parsed into elements
 * by the parser. They are meant to be consumed as a raw data stream
 * by the caller.
 */
public class RawEntity implements EntityStateMachine {

    private final InputStream stream;

    private int state;

    RawEntity(InputStream stream) {
        this.stream = stream;
        this.state = EntityStates.T_RAW_ENTITY;
    }

    public int getState() {
        return state;
    }

    /**
     * This method has no effect.
     */
    public void setRecursionMode(int recursionMode) {
    }

    public EntityStateMachine advance() {
        state = EntityStates.T_END_OF_STREAM;
        return null;
    }

    /**
     * Returns raw data stream.
     */
    public InputStream getContentStream() {
        return stream;
    }

    /**
     * This method has no effect and always returns <code>null</code>.
     */
    public BodyDescriptor getBodyDescriptor() {
        return null;
    }

    /**
     * This method has no effect and always returns <code>null</code>.
     */
    public RawField getField() {
        return null;
    }

    /**
     * This method has no effect and always returns <code>null</code>.
     */
    public String getFieldName() {
        return null;
    }

    /**
     * This method has no effect and always returns <code>null</code>.
     */
    public String getFieldValue() {
        return null;
    }

    /**
     * @see org.apache.james.mime4j.stream.EntityStateMachine#getDecodedContentStream()
     */
    public InputStream getDecodedContentStream() throws IllegalStateException {
        throw new IllegalStateException("Raw entity does not support stream decoding");
    }

}
