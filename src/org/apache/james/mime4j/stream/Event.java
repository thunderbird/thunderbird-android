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

/**
 * Enumerates events which can be monitored.
 */
public final class Event {

    /** Indicates that a body part ended prematurely. */
    public static final Event MIME_BODY_PREMATURE_END
        = new Event("Body part ended prematurely. " +
                "Boundary detected in header or EOF reached.");
    /** Indicates that unexpected end of headers detected.*/
    public static final Event HEADERS_PREMATURE_END
        = new Event("Unexpected end of headers detected. " +
                "Higher level boundary detected or EOF reached.");
    /** Indicates that unexpected end of headers detected.*/
    public static final Event INVALID_HEADER
        = new Event("Invalid header encountered");
    /** Indicates that an obsolete syntax header has been detected */
    public static final Event OBSOLETE_HEADER
        = new Event("Obsolete header encountered");

    private final String code;

    public Event(final String code) {
        super();
        if (code == null) {
            throw new IllegalArgumentException("Code may not be null");
        }
        this.code = code;
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof Event) {
            Event that = (Event) obj;
            return this.code.equals(that.code);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return code;
    }

}