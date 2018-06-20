package com.fsck.k9.fragment;


import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.ui.ContactBadge;


public class MessageViewHolder implements View.OnClickListener {
    private final MessageListFragment fragment;
    public TextView subject;
    public TextView preview;
    public TextView from;
    public TextView time;
    public TextView date;
    public View chip;
    public TextView threadCount;
    public CheckBox flagged;
    public CheckBox selected;
    public int position = -1;
    public ContactBadge contactBadge;

    public MessageViewHolder(MessageListFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onClick(View view) {
        if (position != -1) {
            switch (view.getId()) {
                case R.id.selected_checkbox:
                    fragment.toggleMessageSelectWithAdapterPosition(position);
                    break;
                case R.id.flagged_bottom_right:
                case R.id.flagged_center_right:
                    fragment.toggleMessageFlagWithAdapterPosition(position);
                    break;
            }
        }
    }
}
