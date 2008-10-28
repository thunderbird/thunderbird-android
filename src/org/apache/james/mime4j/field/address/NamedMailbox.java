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

/**
 * A Mailbox that has a name/description.
 *
 * 
 */
public class NamedMailbox extends Mailbox {
	private String name;

	/**
	 * @see Mailbox#Mailbox(String, String)
	 */
	public NamedMailbox(String name, String localPart, String domain) {
		super(localPart, domain);
		this.name = name;
	}

	/**
	 * @see Mailbox#Mailbox(DomainList, String, String)
	 */
	public NamedMailbox(String name, DomainList route, String localPart, String domain) {
		super(route, localPart, domain);
		this.name = name;
	}
	
	/**
	 * Creates a named mailbox based on an unnamed mailbox. 
	 */
	public NamedMailbox(String name, Mailbox baseMailbox) {
		super(baseMailbox.getRoute(), baseMailbox.getLocalPart(), baseMailbox.getDomain());
		this.name = name;
	}

	/**
	 * Returns the name of the mailbox. 
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Same features (or problems) as Mailbox.getAddressString(boolean),
	 * only more so.
	 * 
	 * @see Mailbox#getAddressString(boolean) 
	 */
	public String getAddressString(boolean includeRoute) {
		return (name == null ? "" : name + " ") + super.getAddressString(includeRoute);
	}
}
