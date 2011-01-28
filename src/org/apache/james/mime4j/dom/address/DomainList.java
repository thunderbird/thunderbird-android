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

package org.apache.james.mime4j.dom.address;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable, random-access list of Strings (that are supposedly domain names
 * or domain literals).
 */
public class DomainList extends AbstractList<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> domains;

    /**
     * @param domains
     *            A List that contains only String objects.
     * @param dontCopy
     *            true iff it is not possible for the domains list to be
     *            modified by someone else.
     */
    public DomainList(List<String> domains, boolean dontCopy) {
        if (domains != null)
            this.domains = dontCopy ? domains : new ArrayList<String>(domains);
        else
            this.domains = Collections.emptyList();
    }

    /**
     * The number of elements in this list.
     */
    @Override
    public int size() {
        return domains.size();
    }

    /**
     * Gets the domain name or domain literal at the specified index.
     *
     * @throws IndexOutOfBoundsException
     *             If index is &lt; 0 or &gt;= size().
     */
    @Override
    public String get(int index) {
        return domains.get(index);
    }

    /**
     * Returns the list of domains formatted as a route string (not including
     * the trailing ':').
     */
    public String toRouteString() {
        StringBuilder sb = new StringBuilder();

        for (String domain : domains) {
            if (sb.length() > 0) {
                sb.append(',');
            }

            sb.append("@");
            sb.append(domain);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return toRouteString();
    }

}
