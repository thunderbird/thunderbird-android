package com.fsck.k9.preferences;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class CheckBoxListPreference extends DialogPreference {

    private CharSequence[] items;

    private boolean[] checkedItems;

    /**
     * checkboxes state when the dialog is displayed
     */
    private boolean[] pendingItems;

    public CheckBoxListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(final Builder builder) {
        pendingItems = new boolean[items.length];

        System.arraycopy(checkedItems, 0, pendingItems, 0, checkedItems.length);

        builder.setMultiChoiceItems(items, pendingItems,
        new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which,
            final boolean isChecked) {
                pendingItems[which] = isChecked;
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            System.arraycopy(pendingItems, 0, checkedItems, 0, pendingItems.length);
        }
        pendingItems = null;
    }

    public void setItems(final CharSequence[] items) {
        this.items = items;
    }

    public void setCheckedItems(final boolean[] items) {
        checkedItems = items;
    }

    public boolean[] getCheckedItems() {
        return checkedItems;
    }

}
