package com.fsck.k9.ui;

import android.content.res.Resources;
import com.fsck.k9.RobolectricTest;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

public class K9DrawerTest extends RobolectricTest {

    @Test
    public void testAccountColorLengthEqualsDrawerColorLength() {
        Resources res = RuntimeEnvironment.application.getResources();
        int[] lightColors = res.getIntArray(R.array.account_colors);
        int[] darkColors = res.getIntArray(R.array.drawer_account_accent_color_dark_theme);
        assertEquals(lightColors.length, darkColors.length);
    }
}
