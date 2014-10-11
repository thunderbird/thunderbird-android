package com.fsck.k9.mail.internet;

import com.fsck.k9.helper.Utility;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.LinkedList;

public class UtilityTest extends TestCase {

    public void testToSerializableList() {
        LinkedList<String> input = new LinkedList<String>(Arrays.asList("a", "b"));

        Serializable serializableList = Utility.toSerializableList(input);

        assertEquals(serializableList, input);
    }

    public void testToSerializableListAlreadySerializable() {
        ArrayList<String> input = new ArrayList<String>(Arrays.asList("a", "b"));

        Serializable serializableList = Utility.toSerializableList(input);

        assertSame(serializableList, input);
    }

}
