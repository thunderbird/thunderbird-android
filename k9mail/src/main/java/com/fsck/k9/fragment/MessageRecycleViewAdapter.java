package com.fsck.k9.fragment;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
/**
 Created by Kamil Rajtar on 05.11.17. */

public class MessageRecycleViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
	private static final int TYPE_MESSAGE_ITEM=0;
	private static final int TYPE_FOOTER_ITEM=1;

	private final Context context;
	private final CursorAdapter internalAdapter;
	private final IOnMessageClickListener onMessageClickListener;
	private final ICursorContextMenuSupplier cursorContextMenuSupplier;
	private View footerView;

	public MessageRecycleViewAdapter(final Context context,
									 final MessageListFragment messageListFragment,
									 final IOnMessageClickListener onMessageClickListener,
									 final ICursorContextMenuSupplier cursorContextMenuSupplier){
		this.context=context;
		this.internalAdapter=new MessageListInternalAdapter(messageListFragment);
		this.onMessageClickListener=onMessageClickListener;
		this.cursorContextMenuSupplier=cursorContextMenuSupplier;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent,final int viewType){
		switch(viewType){
			case TYPE_MESSAGE_ITEM:
				final View v=internalAdapter.newView(context,internalAdapter.getCursor(),parent);
				return new MessageViewHolder(v);
			case TYPE_FOOTER_ITEM:
				return new FooterViewHolder(footerView);
		}
		throw new RuntimeException("Unknown view type: "+viewType);
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder holder,final int position){
		if(position==getCount())
			return;
		internalAdapter.getCursor().moveToPosition(position);
		internalAdapter.bindView(holder.itemView,context,internalAdapter.getCursor());
		holder.itemView.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(final View v){
				final Cursor cursor=internalAdapter.getCursor();
				final int adapterPosition=holder.getAdapterPosition();
				cursor.moveToPosition(adapterPosition);
				onMessageClickListener.onMessageClick(internalAdapter.getCursor(),adapterPosition);
			}
		});
		holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(final ContextMenu menu,final View v,
											final ContextMenu.ContextMenuInfo menuInfo){
				final Cursor cursor=internalAdapter.getCursor();
				final int adapterPosition=holder.getAdapterPosition();
				cursor.moveToPosition(adapterPosition);

				cursorContextMenuSupplier.getCursorMenu(menu,cursor);
			}
		});
	}

	public int getCount(){
		return internalAdapter.getCount();
	}

	@Override
	public int getItemViewType(final int position){
		if(position==getCount())
			return TYPE_FOOTER_ITEM;
		return TYPE_MESSAGE_ITEM;
	}

	@Override
	public int getItemCount(){
		return internalAdapter.getCount()+1;
	}

	public Object getItem(final int adapterPosition){
		return internalAdapter.getItem(adapterPosition);
	}

	public void swapCursor(final Cursor cursor){
		internalAdapter.swapCursor(cursor);
		notifyDataSetChanged();
	}

	public boolean isEmpty(){
		return internalAdapter.isEmpty();
	}

	public void delete(final int adapterPosition){
		Log.e("RecyclerViewAdapter","DELETING:"+adapterPosition);
	}

	public void addFooterView(final View footerView){
		this.footerView=footerView;
		notifyDataSetChanged();
	}

	public class MessageViewHolder extends RecyclerView.ViewHolder{
		public MessageViewHolder(final View itemView){
			super(itemView);
		}
	}

	public class FooterViewHolder extends RecyclerView.ViewHolder{
		public FooterViewHolder(final View itemView){
			super(itemView);
		}
	}
}
