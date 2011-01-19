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

/**
 * Constants for common header field names.
 */
public class FieldName {

    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String CONTENT_TYPE = "Content-Type";

    public static final String DATE = "Date";
    public static final String MESSAGE_ID = "Message-ID";
    public static final String SUBJECT = "Subject";

    public static final String FROM = "From";
    public static final String SENDER = "Sender";
    public static final String TO = "To";
    public static final String CC = "Cc";
    public static final String BCC = "Bcc";
    public static final String REPLY_TO = "Reply-To";

    public static final String RESENT_DATE = "Resent-Date";

    public static final String RESENT_FROM = "Resent-From";
    public static final String RESENT_SENDER = "Resent-Sender";
    public static final String RESENT_TO = "Resent-To";
    public static final String RESENT_CC = "Resent-Cc";
    public static final String RESENT_BCC = "Resent-Bcc";

    private FieldName() {
    }

}
