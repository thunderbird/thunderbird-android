package com.fsck.k9.mail;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Flag class. TODO move to a test folder in k9mail-library.
 */
@RunWith(AndroidJUnit4.class)
public class FlagTest {

    private static final Comparator<Flag> FLAG_COMPARATOR = new Comparator<Flag>() {
        @Override
        public int compare(Flag lhs, Flag rhs) {
            return lhs.name().compareTo(rhs.name());
        }
    };

    @Test
    public void testValueOf() throws IllegalAccessException {
        List<Flag> declaredFlagFields = new ArrayList<Flag>();

        for (Field field : Flag.class.getDeclaredFields()) {
            if (field.getType().equals(Flag.class) && Modifier.isStatic(field.getModifiers())) {
                declaredFlagFields.add((Flag) field.get(null));
            }
        }

        Collections.sort(declaredFlagFields, FLAG_COMPARATOR);

        List<Flag> methodFlagFields = Flag.knownFlags();

        Collections.sort(methodFlagFields, FLAG_COMPARATOR);

        assertEquals(declaredFlagFields, methodFlagFields);
    }
}
