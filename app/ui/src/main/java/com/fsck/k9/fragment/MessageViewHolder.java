package com.fsck.k9.fragment;


import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.fsck.k9.ui.ContactBadge;
import com.fsck.k9.ui.R;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class MessageViewHolder implements View.OnClickListener {
    private final Function1<Integer, Unit> toggleMessageFlagWithAdapterPosition;
    private final Function1<Integer, Unit> toggleMessageSelectWithAdapterPosition;
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
    public ImageView attachment;
    public ImageView status;

    public MessageViewHolder(
            Function1<Integer, Unit> toggleMessageSelectWithAdapterPosition,
            Function1<Integer, Unit> toggleMessageFlagWithAdapterPosition
    ) {
        this.toggleMessageSelectWithAdapterPosition = toggleMessageSelectWithAdapterPosition;
        this.toggleMessageFlagWithAdapterPosition = toggleMessageFlagWithAdapterPosition;
    }

    @Override
    public void onClick(View view) {
        if (position != -1) {
            int id = view.getId();
            if (id == R.id.selected_checkbox) {
                toggleMessageSelectWithAdapterPosition.invoke(position);
            } else if (id == R.id.star) {
                toggleMessageFlagWithAdapterPosition.invoke(position);
            }
        }
    }
}
