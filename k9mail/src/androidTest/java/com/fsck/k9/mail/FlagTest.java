package com.fsck.k9.mail;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Flag class. TODO move to a test folder in k9mail-library.
 */
@RunWith(AndroidJUnit4.class)
public class FlagTest {

    @Test
    public void testValueOf() throws IllegalAccessException {
        Set<Flag> declaredFlagFields = new HashSet<Flag>();

        for (Field field : Flag.class.getDeclaredFields()) {
            if (field.getType().equals(Flag.class) && Modifier.isStatic(field.getModifiers())) {
                declaredFlagFields.add((Flag) field.get(null));
            }
        }

        assertEquals(declaredFlagFields, Flag.KNOWN_FLAGS);
    }
}
