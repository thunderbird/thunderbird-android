package com.fsck.k9.mailstore.migrations;


import android.content.ContentValues;

import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mailstore.migrations.MigrationTo51.MimeStructureState;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(K9RobolectricTestRunner.class) // required for ContentValues
public class MigrationMimeStructureStateTest {

    @Test(expected = IllegalStateException.class)
    public void init_popParent_shouldCrash() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        state.popParent();
    }

    @Test(expected = IllegalStateException.class)
    public void init_apply_apply_shouldCrash() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);

        state.applyValues(cv);
    }

    @Test(expected = IllegalStateException.class)
    public void init_nextchild_shouldCrash() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        state.nextChild(1);
    }

    @Test(expected = IllegalStateException.class)
    public void init_nextmulti_shouldCrash() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        state.nextMultipartChild(1);
    }

    @Test(expected = IllegalStateException.class)
    public void init_apply_nextmulti_nextchild_shouldCrash() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);
        state.nextMultipartChild(1);

        state.nextChild(1);
    }

    @Test(expected = IllegalStateException.class)
    public void init_apply_nextchild_nextmulti_shouldCrash() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);
        state.nextChild(1);

        state.nextMultipartChild(1);
    }

    @Test
    public void init_apply_shouldYieldStartValues() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);

        Assert.assertEquals(-1L, cv.get("parent"));
        Assert.assertEquals(0, cv.get("seq"));
        Assert.assertEquals(2, cv.size());
    }

    @Test(expected = IllegalStateException.class)
    public void init_apply_nextchild_apply_shouldCrash() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);
        state = state.nextChild(123);
        cv.clear();

        state.applyValues(cv);
    }

    @Test
    public void init_apply_nextmulti_apply_shouldYieldMultipartChildValues() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);
        state = state.nextMultipartChild(123);
        cv.clear();
        state.applyValues(cv);

        Assert.assertEquals(123L, cv.get("root"));
        Assert.assertEquals(123L, cv.get("parent"));
        Assert.assertEquals(1, cv.get("seq"));
        Assert.assertEquals(3, cv.size());
    }

    @Test
    public void init_apply_nextmulti_apply_nextmulti_apply_shouldYieldSecondMultipartChildValues() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);
        state = state.nextMultipartChild(123);
        cv.clear();
        state.applyValues(cv);
        state = state.nextMultipartChild(456);
        cv.clear();
        state.applyValues(cv);

        Assert.assertEquals(123L, cv.get("root"));
        Assert.assertEquals(456L, cv.get("parent"));
        Assert.assertEquals(2, cv.get("seq"));
        Assert.assertEquals(3, cv.size());
    }

    @Test
    public void init_apply_nextmulti_apply_pop_apply_shouldYieldFirstParentIdValues() throws Exception {
        MimeStructureState state = MimeStructureState.getNewRootState();

        ContentValues cv = new ContentValues();
        state.applyValues(cv);
        state = state.nextMultipartChild(123);
        cv.clear();
        state.applyValues(cv);
        state = state.nextMultipartChild(456);
        state = state.popParent();
        cv.clear();
        state.applyValues(cv);

        Assert.assertEquals(123L, cv.get("root"));
        Assert.assertEquals(123L, cv.get("parent"));
        Assert.assertEquals(2, cv.get("seq"));
        Assert.assertEquals(3, cv.size());
    }
}
