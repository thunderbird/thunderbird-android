package com.fsck.k9.fragment;


import android.database.Cursor;
import android.view.ContextMenu;


public interface CursorContextMenuSupplier {
    void getCursorMenu(ContextMenu menu, Cursor cursor);
}