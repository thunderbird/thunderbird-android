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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a MIME multipart body (see RFC 2045).A multipart body has a
 * ordered list of body parts. The multipart body also has a preamble and
 * epilogue. The preamble consists of whatever characters appear before the
 * first body part while the epilogue consists of whatever characters come after
 * the last body part.
 */
public abstract class Multipart implements Body {

    protected List<Entity> bodyParts = new LinkedList<Entity>();
    private Entity parent = null;

    private String subType;

    /**
     * Creates a new empty <code>Multipart</code> instance.
     */
    public Multipart(String subType) {
        this.subType = subType;
    }

    /**
     * Gets the multipart sub-type. E.g. <code>alternative</code> (the
     * default) or <code>parallel</code>. See RFC 2045 for common sub-types
     * and their meaning.
     *
     * @return the multipart sub-type.
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Sets the multipart sub-type. E.g. <code>alternative</code> or
     * <code>parallel</code>. See RFC 2045 for common sub-types and their
     * meaning.
     *
     * @param subType
     *            the sub-type.
     */
    public void setSubType(String subType) {
        this.subType = subType;
    }

    /**
     * @see org.apache.james.mime4j.dom.Body#getParent()
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * @see org.apache.james.mime4j.dom.Body#setParent(org.apache.james.mime4j.dom.Entity)
     */
    public void setParent(Entity parent) {
        this.parent = parent;
        for (Entity bodyPart : bodyParts) {
            bodyPart.setParent(parent);
        }
    }

    /**
     * Returns the number of body parts.
     *
     * @return number of <code>Entity</code> objects.
     */
    public int getCount() {
        return bodyParts.size();
    }

    /**
     * Gets the list of body parts. The list is immutable.
     *
     * @return the list of <code>Entity</code> objects.
     */
    public List<Entity> getBodyParts() {
        return Collections.unmodifiableList(bodyParts);
    }

    /**
     * Sets the list of body parts.
     *
     * @param bodyParts
     *            the new list of <code>Entity</code> objects.
     */
    public void setBodyParts(List<Entity> bodyParts) {
        this.bodyParts = bodyParts;
        for (Entity bodyPart : bodyParts) {
            bodyPart.setParent(parent);
        }
    }

    /**
     * Adds a body part to the end of the list of body parts.
     *
     * @param bodyPart
     *            the body part.
     */
    public void addBodyPart(Entity bodyPart) {
        if (bodyPart == null)
            throw new IllegalArgumentException();

        bodyParts.add(bodyPart);
        bodyPart.setParent(parent);
    }

    /**
     * Inserts a body part at the specified position in the list of body parts.
     *
     * @param bodyPart
     *            the body part.
     * @param index
     *            index at which the specified body part is to be inserted.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index &lt; 0 || index &gt;
     *             getCount()).
     */
    public void addBodyPart(Entity bodyPart, int index) {
        if (bodyPart == null)
            throw new IllegalArgumentException();

        bodyParts.add(index, bodyPart);
        bodyPart.setParent(parent);
    }

    /**
     * Removes the body part at the specified position in the list of body
     * parts.
     *
     * @param index
     *            index of the body part to be removed.
     * @return the removed body part.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index &lt; 0 || index &gt;=
     *             getCount()).
     */
    public Entity removeBodyPart(int index) {
        Entity bodyPart = bodyParts.remove(index);
        bodyPart.setParent(null);
        return bodyPart;
    }

    /**
     * Replaces the body part at the specified position in the list of body
     * parts with the specified body part.
     *
     * @param bodyPart
     *            body part to be stored at the specified position.
     * @param index
     *            index of body part to replace.
     * @return the replaced body part.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index &lt; 0 || index &gt;=
     *             getCount()).
     */
    public Entity replaceBodyPart(Entity bodyPart, int index) {
        if (bodyPart == null)
            throw new IllegalArgumentException();

        Entity replacedEntity = bodyParts.set(index, bodyPart);
        if (bodyPart == replacedEntity)
            throw new IllegalArgumentException(
                    "Cannot replace body part with itself");

        bodyPart.setParent(parent);
        replacedEntity.setParent(null);

        return replacedEntity;
    }

    /**
     * Gets the preamble or null if the message has no preamble.
     *
     * @return the preamble.
     */
    public abstract String getPreamble();

    /**
     * Sets the preamble with a value or null to remove the preamble.
     *
     * @param preamble
     *            the preamble.
     */
    public abstract void setPreamble(String preamble);

    /**
     * Gets the epilogue or null if the message has no epilogue
     *
     * @return the epilogue.
     */
    public abstract String getEpilogue();

    /**
     * Sets the epilogue value, or remove it if the value passed is null.
     *
     * @param epilogue
     *            the epilogue.
     */
    public abstract void setEpilogue(String epilogue);

    /**
     * Disposes of the BodyParts of this Multipart. Note that the dispose call
     * does not get forwarded to the parent entity of this Multipart.
     *
     * @see org.apache.james.mime4j.dom.Disposable#dispose()
     */
    public void dispose() {
        for (Entity bodyPart : bodyParts) {
            bodyPart.dispose();
        }
    }

}
