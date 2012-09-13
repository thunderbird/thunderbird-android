
package com.fsck.k9.helper;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuPopupHelper;
public class MenuPopup extends MenuPopupHelper {

    OnMenuItemClickListener onMenuItemClickListener;

    public MenuPopup(Context context, MenuBuilder menu, View anchorView) {
        super(context, menu, anchorView);
    }

    public void setOnMenuItemClickListener(
            OnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        super.onItemClick(parent, view, position, id);
        if (onMenuItemClickListener != null)
            onMenuItemClickListener.onMenuItemClick(position);
    }

    public interface OnMenuItemClickListener{
        public void onMenuItemClick(int itemID);
    }
}
