package com.fsck.k9.helper;

import android.content.Context;
import android.view.View;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuPopupHelper;
import com.actionbarsherlock.view.MenuItem;

public class MenuPopup extends MenuPopupHelper {

    private MenuBuilder mMenu;


    public MenuPopup(Context context, MenuBuilder menu, View anchorView) {
        super(context, menu, anchorView);
        mMenu = menu;
    }

    public void setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener listener) {
        for (int i = 0, end = mMenu.size(); i < end; i++) {
            mMenu.getItem(i).setOnMenuItemClickListener(listener);
        }
    }
}
