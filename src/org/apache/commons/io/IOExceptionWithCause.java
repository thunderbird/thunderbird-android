/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.io;

import java.io.IOException;

/**
 * Subclasses IOException with the {@link Throwable} constructors missing before Java 6. If you are using Java 6,
 * consider this class deprecated and use {@link IOException}.
 * 
 * @author <a href="http://commons.apache.org/io/">Apache Commons IO</a>
 * @version $Id$
 * @since Commons IO 1.4
 */
public class IOExceptionWithCause extends IOException {

    /**
     * Defines the serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new instance with the given message and cause.
     * <p>
     * As specified in {@link Throwable}, the message in the given <code>cause</code> is not used in this instance's
     * message.
     * </p>
     * 
     * @param message
     *            the message (see {@link #getMessage()})
     * @param cause
     *            the cause (see {@link #getCause()}). A <code>null</code> value is allowed.
     */
    public IOExceptionWithCause(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }

    /**
     * Constructs a new instance with the given cause.
     * <p>
     * The message is set to <code>cause==null ? null : cause.toString()</code>, which by default contains the class
     * and message of <code>cause</code>. This constructor is useful for call sites that just wrap another throwable.
     * </p>
     * 
     * @param cause
     *            the cause (see {@link #getCause()}). A <code>null</code> value is allowed.
     */
    public IOExceptionWithCause(Throwable cause) {
        super(cause == null ? null : cause.toString());
        this.initCause(cause);
    }

}
