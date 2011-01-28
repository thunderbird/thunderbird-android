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
package org.apache.james.mime4j.dom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

/**
 * Utility class to load Service Providers (SPI).
 * This will deprecated as soon as mime4j will be upgraded to Java6
 * as Java6 has javax.util.ServiceLoader as a core class.
 */
class ServiceLoader {

    private ServiceLoader() {
    }

    /**
     * Loads a Service Provider for the given interface/class (SPI).
     */
    static <T> T load(Class<T> spiClass) {
        String spiResURI = "META-INF/services/" + spiClass.getName();
        ClassLoader classLoader = spiClass.getClassLoader();
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(spiResURI);
        } catch (IOException e) {
            return null;
        }

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(resource
                        .openStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    int cmtIdx = line.indexOf('#');
                    if (cmtIdx != -1) {
                        line = line.substring(0, cmtIdx);
                        line = line.trim();
                    }

                    if (line.length() == 0) {
                        continue;
                    }

                    Class<?> implClass;
                    try {
                        implClass = classLoader.loadClass(line);

                        if (spiClass.isAssignableFrom(implClass)) {
                            Object impl = implClass.newInstance();
                            return spiClass.cast(impl);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        return null;
    }
}