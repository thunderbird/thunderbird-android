package com.fsck.k9.fragment;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;


public class MessageListRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_MESSAGE_ITEM = 0;
    private static final int TYPE_FOOTER_ITEM = 1;

    private final Context context;
    private final CursorAdapter internalAdapter;
    private final OnMessageClickListener onMessageClickListener;
    private final CursorContextMenuSupplier cursorContextMenuSupplier;
    private int selectedItemPosition = RecyclerView.NO_POSITION;
    private FooterViewHolder footerViewHolder;

    public MessageListRecyclerAdapter(Context context,
            CursorAdapter internalAdapter,
            OnMessageClickListener onMessageClickListener,
            CursorContextMenuSupplier cursorContextMenuSupplier) {
        this.context = context;
        this.internalAdapter = internalAdapter;
        this.onMessageClickListener = onMessageClickListener;
        this.cursorContextMenuSupplier = cursorContextMenuSupplier;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_MESSAGE_ITEM:
                View v = internalAdapter.newView(context, internalAdapter.getCursor(), parent);
                return new MessageViewHolder(v);
            case TYPE_FOOTER_ITEM:
                return footerViewHolder;
        }
        throw new RuntimeException("Unknown view type: " + viewType);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (position == getCount()) {
            return;
        }
        internalAdapter.getCursor().moveToPosition(position);
        internalAdapter.bindView(holder.itemView, context, internalAdapter.getCursor());
        if (position == selectedItemPosition) {
            holder.itemView.setBackgroundColor(Color.parseColor("#00BCD4"));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor cursor = internalAdapter.getCursor();
                int adapterPosition = holder.getAdapterPosition();
                cursor.moveToPosition(adapterPosition);
                onMessageClickListener.onMessageClick(internalAdapter.getCursor(), adapterPosition);
            }
        });
        holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenu.ContextMenuInfo menuInfo) {
                Cursor cursor = internalAdapter.getCursor();
                int adapterPosition = holder.getAdapterPosition();
                cursor.moveToPosition(adapterPosition);

                cursorContextMenuSupplier.getCursorMenu(menu, cursor);
            }
        });
    }

    public int getCount() {
        return internalAdapter.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getCount()) {
            return TYPE_FOOTER_ITEM;
        }
        return TYPE_MESSAGE_ITEM;
    }

    @Override
    public int getItemCount() {
        return internalAdapter.getCount() + 1;
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        recyclerView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        Log.v("RecycleAdapter", "Enter. Selected item:" + selectedItemPosition);
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                tryMoveSelection(recyclerView, 1);

                                Log.v("RecycleAdapter", "Exit. Selected item:" + selectedItemPosition);
                                return true;
                            case KeyEvent.KEYCODE_DPAD_UP:
                                tryMoveSelection(recyclerView, -1);
                                Log.v("RecycleAdapter", "Exit. Selected item:" + selectedItemPosition);
                                return true;
                        }
                }
                return false;
            }
        });
    }

    private void tryMoveSelection(RecyclerView recyclerView, int direction) {
        int nextSelectItem = selectedItemPosition + direction;

        if (nextSelectItem >= 0 && nextSelectItem < getItemCount()) {
            notifyItemChanged(selectedItemPosition);
            selectedItemPosition = nextSelectItem;
            notifyItemChanged(selectedItemPosition);
            recyclerView.scrollToPosition(selectedItemPosition);
        }
    }

    public int getSelectedItemPosition() {
        return selectedItemPosition;
    }

    public Object getItem(int adapterPosition) {
        return internalAdapter.getItem(adapterPosition);
    }

    public void swapCursor(Cursor cursor) {
        internalAdapter.swapCursor(cursor);
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return internalAdapter.isEmpty();
    }

    public void addFooterView(View footerView) {
        footerViewHolder = new FooterViewHolder(footerView);
        notifyDataSetChanged();
    }

    public void setSelectedItemPosition(int position) {
        this.selectedItemPosition = position;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        public MessageViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}