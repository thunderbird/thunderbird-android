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
package org.apache.james.mime4j.message;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageBuilderFactory;
import org.apache.james.mime4j.storage.StorageProvider;
import org.apache.james.mime4j.stream.MimeEntityConfig;
import org.apache.james.mime4j.stream.MutableBodyDescriptorFactory;

/**
 * The default MessageBuilderFactory bundled with Mime4j.
 *
 * Supports the "StorageProvider", "MimeEntityConfig" and "MutableBodyDescriptorFactory"
 * attributes.
 */
public class MessageBuilderFactoryImpl extends MessageBuilderFactory {

    private StorageProvider storageProvider = null;
    private MimeEntityConfig mimeEntityConfig = null;
    private MutableBodyDescriptorFactory mutableBodyDescriptorFactory = null;

    @Override
    public MessageBuilder newMessageBuilder() throws MimeException {
        MessageBuilderImpl m = new MessageBuilderImpl();
        if (storageProvider != null) m.setStorageProvider(storageProvider);
        if (mimeEntityConfig != null) m.setMimeEntityConfig(mimeEntityConfig);
        if (mutableBodyDescriptorFactory != null) m.setMutableBodyDescriptorFactory(mutableBodyDescriptorFactory);
        return m;
    }

    @Override
    public void setAttribute(String name, Object value)
            throws IllegalArgumentException {
        if ("StorageProvider".equals(name)) {
            if (value instanceof StorageProvider) {
                this.storageProvider  = (StorageProvider) value;
                return;
            } else throw new IllegalArgumentException("Unsupported attribute value type for "+name+", expected a StorageProvider");
        } else if ("MimeEntityConfig".equals(name)) {
            if (value instanceof MimeEntityConfig) {
                this.mimeEntityConfig = (MimeEntityConfig) value;
                return;
            } else throw new IllegalArgumentException("Unsupported attribute value type for "+name+", expected a MimeEntityConfig");
        } else if ("MutableBodyDescriptorFactory".equals(name)) {
            if (value instanceof MutableBodyDescriptorFactory) {
                this.mutableBodyDescriptorFactory  = (MutableBodyDescriptorFactory) value;
                return;
            } else throw new IllegalArgumentException("Unsupported attribute value type for "+name+", expected a MutableBodyDescriptorFactory");
        }

        throw new IllegalArgumentException("Unsupported attribute: "+name);

    }

}
