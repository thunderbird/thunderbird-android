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

package org.apache.james.mime4j.storage;

/**
 * Allows for a default {@link StorageProvider} instance to be configured on an
 * application level.
 * <p>
 * The default instance can be set by either calling
 * {@link #setInstance(StorageProvider)} when the application starts up or by
 * setting the system property
 * <code>org.apache.james.mime4j.defaultStorageProvider</code> to the class
 * name of a <code>StorageProvider</code> implementation.
 * <p>
 * If neither option is used or if the class instantiation fails this class
 * provides a pre-configured default instance.
 */
public class DefaultStorageProvider {

    /** Value is <code>org.apache.james.mime4j.defaultStorageProvider</code> */
    public static final String DEFAULT_STORAGE_PROVIDER_PROPERTY =
        "org.apache.james.mime4j.defaultStorageProvider";

    private static volatile StorageProvider instance = null;

    static {
        initialize();
    }

    private DefaultStorageProvider() {
    }

    /**
     * Returns the default {@link StorageProvider} instance.
     *
     * @return the default {@link StorageProvider} instance.
     */
    public static StorageProvider getInstance() {
        return instance;
    }

    /**
     * Sets the default {@link StorageProvider} instance.
     *
     * @param instance
     *            the default {@link StorageProvider} instance.
     */
    public static void setInstance(StorageProvider instance) {
        if (instance == null) {
            throw new IllegalArgumentException();
        }

        DefaultStorageProvider.instance = instance;
    }

    private static void initialize() {
        String clazz = System.getProperty(DEFAULT_STORAGE_PROVIDER_PROPERTY);
        try {
            if (clazz != null) {
                instance = (StorageProvider) Class.forName(clazz).newInstance();
            }
        } catch (Exception e) {
        }
        if (instance == null) {
            StorageProvider backend = new TempFileStorageProvider();
            instance = new ThresholdStorageProvider(backend, 1024);
        }
    }

    // for unit tests only
    static void reset() {
        instance = null;
        initialize();
    }

}
