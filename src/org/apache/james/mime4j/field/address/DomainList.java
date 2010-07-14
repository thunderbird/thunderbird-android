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

package org.apache.james.mime4j.field.address;

import java.util.ArrayList;

/**
 * An immutable, random-access list of Strings (that 
 * are supposedly domain names or domain literals).
 *
 * 
 */
public class DomainList {
	private ArrayList domains;
	
	/**
	 * @param domains An ArrayList that contains only String objects. 
	 * @param dontCopy true iff it is not possible for the domains ArrayList to be modified by someone else.
	 */
	public DomainList(ArrayList domains, boolean dontCopy) {
		if (domains != null)
			this.domains = (dontCopy ? domains : (ArrayList) domains.clone());
		else
			this.domains = new ArrayList(0);
	}
	
	/**
	 * The number of elements in this list.
	 */
	public int size() {
		return domains.size();
	}

	/**
	 * Gets the domain name or domain literal at the
	 * specified index.
	 * @throws IndexOutOfBoundsException If index is &lt; 0 or &gt;= size().
	 */
	public String get(int index) {
		if (0 > index || size() <= index)
			throw new IndexOutOfBoundsException();
		return (String) domains.get(index);
	}

	/**
	 * Returns the list of domains formatted as a route
	 * string (not including the trailing ':'). 
	 */
	public String toRouteString() {
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < domains.size(); i++) {
			out.append("@");
			out.append(get(i));
			if (i + 1 < domains.size())
				out.append(",");
		}
		return out.toString();
	}
}
