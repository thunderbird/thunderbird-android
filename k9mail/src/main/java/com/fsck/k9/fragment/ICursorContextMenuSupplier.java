package com.fsck.k9.fragment;
import android.database.Cursor;
import android.view.ContextMenu;
/**
 Created by Kamil Rajtar on 05.11.17. */

public interface ICursorContextMenuSupplier{
	void getCursorMenu(ContextMenu menu,Cursor cursor);
}
