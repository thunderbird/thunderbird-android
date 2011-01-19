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

package org.apache.james.mime4j;

/**
 * MIME processing exception.
 * <p>
 * A <code>MimeException</code> may be thrown by a {@link org.apache.james.mime4j.parser.ContentHandler} to
 * indicate that it has failed to process a message event and that no further
 * events should be generated.
 * <p>
 * <code>MimeException</code> also gets thrown by the parser to indicate MIME
 * protocol errors, e.g. if a message boundary is too long or a header field
 * cannot be parsed.
 */
public class MimeException extends Exception {

    private static final long serialVersionUID = 8352821278714188542L;

    /**
     * Constructs a new MIME exception with the specified detail message.
     *
     * @param message detail message
     */
    public MimeException(String message) {
        super(message);
    }

    /**
     * Constructs a MIME exception with the specified cause.
     *
     * @param cause cause of the exception
     */
    public MimeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a MIME exception with the specified detail message and cause.
     *
     * @param message detail message
     * @param cause cause of the exception
     */
    public MimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
