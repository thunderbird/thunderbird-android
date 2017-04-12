package com.fsck.k9.helper;

import java.util.Comparator;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;

import com.fsck.k9.K9RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MergeCursorTest {

    @Mock
    Cursor mCursor1;
    @Mock
    Cursor mCursor2;

    private Cursor[] mCursors;

    private Comparator<Cursor> mComparator = new Comparator<Cursor>() {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            int o1 = cursor1.getInt(0);
            int o2 = cursor2.getInt(0);

            return o1 - o2;
        }

    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mCursors = new Cursor[2];

        mCursors[0] = mCursor1;
        mCursors[1] = mCursor2;
    }

    @Test
    public void testCursorAreResetOnMergeCursorCreation() {
        reset(mCursor1);
        reset(mCursor2);
        when(mCursor1.moveToPosition(-1)).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                when(mCursor1.getPosition()).thenReturn(-1);
                return true;
            }
        });
        when(mCursor2.moveToPosition(-1)).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                when(mCursor2.getPosition()).thenReturn(-1);
                return true;
            }
        });

        new MergeCursor(mCursors, mComparator);

        assertEquals(-1, mCursor1.getPosition());
        assertEquals(-1, mCursor2.getPosition());
    }

    @Test
    public void testClose() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);
        mergeCursor.close();

        verify(mCursor1).close();
        verify(mCursor2).close();
    }

    @Test
    public void testCopyStringToBuffer() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);
        char[] chars = {'a', 'b', 'c'};
        CharArrayBuffer cab = new CharArrayBuffer(chars);
        mergeCursor.copyStringToBuffer(1, cab);

        verify(mCursor1).copyStringToBuffer(1, cab);
    }

    @Test
    public void testDeactivate() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);
        mergeCursor.deactivate();

        verify(mCursor1).deactivate();
        verify(mCursor2).deactivate();
    }

    @Test
    public void testGetBlob() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);
        mergeCursor.getBlob(1);

        verify(mCursor1).getBlob(1);
    }

    @Test
    public void testGetColumnCount() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getColumnCount()).thenReturn(7);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(7, mergeCursor.getColumnCount());
        verify(mCursor1).getColumnCount();
    }

    @Test
    public void testGetColumnIndex() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getColumnIndex("abc")).thenReturn(2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(2, mergeCursor.getColumnIndex("abc"));
        verify(mCursor1).getColumnIndex("abc");
    }

    @Test
    public void testGetColumnIndexOrThrow() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getColumnIndexOrThrow("abc")).thenReturn(2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(2, mergeCursor.getColumnIndexOrThrow("abc"));
        verify(mCursor1).getColumnIndexOrThrow("abc");
    }

    @Test
    public void testGetColumnName() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getColumnName(2)).thenReturn("abc");

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals("abc", mergeCursor.getColumnName(2));
        verify(mCursor1).getColumnName(2);
    }

    @Test
    public void testGetColumnNames() {
        reset(mCursor1);
        reset(mCursor2);

        String[] strings = new String[3];
        strings[0] = "aaa";
        strings[1] = "bbb";
        strings[2] = "ccc";
        when(mCursor1.getColumnNames()).thenReturn(strings);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(strings, mergeCursor.getColumnNames());
        verify(mCursor1).getColumnNames();
    }

    @Test
    public void testGetCount() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        int sum = mCursor1.getCount() + mCursor2.getCount();
        // before caching
        assertEquals(sum, mergeCursor.getCount());
        // after caching
        assertEquals(sum, mergeCursor.getCount());
    }

    @Test
    public void testGetDouble() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getDouble(2)).thenReturn(3.0);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(3.0, mergeCursor.getDouble(2));
        verify(mCursor1).getDouble(2);
    }

    @Test
    public void testGetFloat() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getFloat(2)).thenReturn(3.0f);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(3.0f, mergeCursor.getFloat(2));
        verify(mCursor1).getFloat(2);
    }

    @Test
    public void testGetInt() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getInt(2)).thenReturn(3);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(3, mergeCursor.getInt(2));
        verify(mCursor1).getInt(2);
    }

    @Test
    public void testGetLong() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getLong(2)).thenReturn(3L);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(3L, mergeCursor.getLong(2));
        verify(mCursor1).getLong(2);
    }

    @Test
    public void testGetShort() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getShort(2)).thenReturn((short) 3);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(3, mergeCursor.getShort(2));
        verify(mCursor1).getShort(2);
    }

    @Test
    public void testGetString() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getString(2)).thenReturn("abc");

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals("abc", mergeCursor.getString(2));
        verify(mCursor1).getString(2);
    }

    @Test
    public void testGetType() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getType(2)).thenReturn(3);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertEquals(3, mergeCursor.getType(2));
        verify(mCursor1).getType(2);
    }

    @Test
    public void testGetWantsAllOnMoveCalls() {
        reset(mCursor1);
        reset(mCursor2);

        when(mCursor1.getWantsAllOnMoveCalls()).thenReturn(true);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        assertTrue(mergeCursor.getWantsAllOnMoveCalls());
        verify(mCursor1).getWantsAllOnMoveCalls();
    }

    @Test
    public void testIsClosed() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        final boolean cursor1Value = true;
        when(mCursor1.isClosed()).thenReturn(cursor1Value);

        assertEquals(cursor1Value, mergeCursor.isClosed());
    }

    @Test
    public void testIsNull() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        when(mCursor1.isNull(1)).thenReturn(false);

        assertEquals(false, mergeCursor.isClosed());
        verify(mCursor1).isClosed();
    }

    @Test
    public void testRegisterContentObserver() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        ContentObserver contentObserver = mock(ContentObserver.class);
        mergeCursor.registerContentObserver(contentObserver);

        verify(mCursor1).registerContentObserver(contentObserver);
        verify(mCursor2).registerContentObserver(contentObserver);
    }

    @Test
    public void testRegisterDataSetObserver() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        DataSetObserver dataSetObserver = mock(DataSetObserver.class);
        mergeCursor.registerDataSetObserver(dataSetObserver);

        verify(mCursor1).registerDataSetObserver(dataSetObserver);
        verify(mCursor2).registerDataSetObserver(dataSetObserver);
    }

    @Test
    public void testRequeryWhenAllCursorsRequeriedSuccessfully() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);
        when(mCursor1.requery()).thenReturn(true);
        when(mCursor2.requery()).thenReturn(true);

        assertTrue(mergeCursor.requery());
        verify(mCursor1).requery();
        verify(mCursor2).requery();
    }

    @Test
    public void testRequeryWhenAtLeastOneCursosRequeriedWithFailure() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);
        when(mCursor1.requery()).thenReturn(false);
        when(mCursor2.requery()).thenReturn(true);

        assertFalse(mergeCursor.requery());
        verify(mCursor1).requery();
        verify(mCursor2).requery();
    }

    @Test
    public void testSetNotificationUri() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        ContentResolver contentResolver = mock(ContentResolver.class);
        Uri uri = mock(Uri.class);
        mergeCursor.setNotificationUri(contentResolver, uri);

        verify(mCursor1).setNotificationUri(contentResolver, uri);
        verify(mCursor2).setNotificationUri(contentResolver, uri);
    }


    @Test
    public void testUnregisterContentObserver() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        ContentObserver contentObserver = mock(ContentObserver.class);
        mergeCursor.unregisterContentObserver(contentObserver);

        verify(mCursor1).unregisterContentObserver(contentObserver);
        verify(mCursor2).unregisterContentObserver(contentObserver);
    }

    @Test
    public void testUnregisterDataSetObserver() {
        reset(mCursor1);
        reset(mCursor2);

        MergeCursor mergeCursor = new MergeCursor(mCursors, mComparator);

        DataSetObserver dataSetObserver = mock(DataSetObserver.class);
        mergeCursor.unregisterDataSetObserver(dataSetObserver);

        verify(mCursor1).unregisterDataSetObserver(dataSetObserver);
        verify(mCursor2).unregisterDataSetObserver(dataSetObserver);
    }

}
