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
package org.apache.commons.io.comparator;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Reverses the result of comparing two objects using
 * the delegate {@link Comparator}.
 *
 * @version $Revision: 609243 $ $Date: 2008-01-06 00:30:42 +0000 (Sun, 06 Jan 2008) $
 * @since Commons IO 1.4
 */
class ReverseComparator implements Comparator, Serializable {

    private final Comparator delegate;

    /**
     * Construct an instance with the sepecified delegate {@link Comparator}.
     *
     * @param delegate The comparator to delegate to
     */
    public ReverseComparator(Comparator delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate comparator is missing");
        }
        this.delegate = delegate;
    }

    /**
     * Compare using the delegate Comparator, but reversing the result.
     * 
     * @param obj1 The first object to compare
     * @param obj2 The second object to compare
     * @return the result from the delegate {@link Comparator#compare(Object, Object)}
     * reversing the value (i.e. positive becomes negative and vice versa)
     */
    public int compare(Object obj1, Object obj2) {
        return delegate.compare(obj2, obj1); // parameters switched round
    }

}
