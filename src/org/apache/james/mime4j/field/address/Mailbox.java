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
 * Represents a single e-mail address. 
 *
 * 
 */
public class Mailbox extends Address {
	private DomainList route;
	private String localPart;
	private String domain;

	/**
	 * Creates a mailbox without a route. Routes are obsolete.
	 * @param localPart The part of the e-mail address to the left of the "@".
	 * @param domain The part of the e-mail address to the right of the "@".
	 */
	public Mailbox(String localPart, String domain) {
		this(null, localPart, domain);
	}
	
	/**
	 * Creates a mailbox with a route. Routes are obsolete.
	 * @param route The zero or more domains that make up the route. Can be null.
	 * @param localPart The part of the e-mail address to the left of the "@".
	 * @param domain The part of the e-mail address to the right of the "@".
	 */
	public Mailbox(DomainList route, String localPart, String domain) {
		this.route = route;
		this.localPart = localPart;
		this.domain = domain;
	}

	/**
	 * Returns the route list.
	 */
	public DomainList getRoute() {
		return route;
	}

	/**
	 * Returns the left part of the e-mail address 
	 * (before "@").
	 */
	public String getLocalPart() {
		return localPart;
	}
	
	/**
	 * Returns the right part of the e-mail address 
	 * (after "@").
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Formats the address as a string, not including
	 * the route.
	 * 
	 * @see #getAddressString(boolean)
	 */
	public String getAddressString() {
		return getAddressString(false);
	}
	
	/**
	 * Note that this value may not be usable
	 * for transport purposes, only display purposes.
	 * 
	 * For example, if the unparsed address was
	 * 
	 *   <"Joe Cheng"@joecheng.com>
	 * 
	 * this method would return
	 * 
	 *   <Joe Cheng@joecheng.com>
	 * 
	 * which is not valid for transport; the local part
	 * would need to be re-quoted.
	 * 
	 * @param includeRoute true if the route should be included if it exists. 
	 */
	public String getAddressString(boolean includeRoute) {
		return "<" + (!includeRoute || route == null ? "" : route.toRouteString() + ":") 
			+ localPart
			+ (domain == null ? "" : "@") 
			+ domain + ">";  
	}
	
	protected final void doAddMailboxesTo(ArrayList results) {
		results.add(this);
	}
	
	public String toString() {
		return getAddressString();
	}
}
