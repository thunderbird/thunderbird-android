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


public interface ParsedField extends Field {

    /**
     * Returns <code>true</code> if this field is valid, i.e. no errors were
     * encountered while parsing the field value.
     *
     * @return <code>true</code> if this field is valid, <code>false</code>
     *         otherwise.
     * @see #getParseException()
     */
    boolean isValidField();

    /**
     * Returns the exception that was thrown by the field parser while parsing
     * the field value. The result is <code>null</code> if the field is valid
     * and no errors were encountered.
     *
     * @return the exception that was thrown by the field parser or
     *         <code>null</code> if the field is valid.
     */
    ParseException getParseException();

}
