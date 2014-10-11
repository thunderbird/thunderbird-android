package com.fsck.k9.mail.internet;

import com.fsck.k9.helper.Utility;

import junit.framework.TestCase;

import java.io.*;
import java.util.*;

public class UtilityTest extends TestCase {

    public void testToArrayList() {
        LinkedList<String> input = new LinkedList<String>(Arrays.asList("a", "b"));

        Serializable serializableList = Utility.toArrayList(input);

        assertEquals(serializableList, input);
    }

    public void testToArrayListAlreadyArrayList() {
        ArrayList<String> input = new ArrayList<String>(Arrays.asList("a", "b"));

        Serializable serializableList = Utility.toArrayList(input);

        assertSame(serializableList, input);
    }

}
