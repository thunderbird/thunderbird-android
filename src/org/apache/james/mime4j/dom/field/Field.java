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

package org.apache.james.mime4j.dom.field;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Abstract MIME field.
 */
public interface Field {

    /**
     * Gets the name of the field (<code>Subject</code>, <code>From</code>, etc).
     *
     * @return the field name.
     */
    String getName();

    /**
     * Gets the unparsed and possibly encoded (see RFC 2047) field body string.
     *
     * @return the unparsed field body string.
     */
    String getBody();

    /**
     * Writes the original raw field bytes to an output stream.
     * The output is folded, the last CRLF is not included.
     * @throws IOException
     */
    void writeTo(OutputStream out) throws IOException;

}
