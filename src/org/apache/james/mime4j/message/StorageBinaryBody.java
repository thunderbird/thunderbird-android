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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.james.mime4j.codec.CodecUtil;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.storage.MultiReferenceStorage;

/**
 * Binary body backed by a
 * {@link org.apache.james.mime4j.storage.Storage}
 */
class StorageBinaryBody extends BinaryBody {

    private MultiReferenceStorage storage;

    public StorageBinaryBody(final MultiReferenceStorage storage) {
        this.storage = storage;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return storage.getInputStream();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        if (out == null)
            throw new IllegalArgumentException();

        InputStream in = storage.getInputStream();
        CodecUtil.copy(in, out);
        in.close();
    }

    @Override
    public StorageBinaryBody copy() {
        storage.addReference();
        return new StorageBinaryBody(storage);
    }

    /**
     * Deletes the Storage that holds the content of this binary body.
     *
     * @see org.apache.james.mime4j.dom.Disposable#dispose()
     */
    @Override
    public void dispose() {
        if (storage != null) {
            storage.delete();
            storage = null;
        }
    }

}
