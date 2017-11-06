package com.fsck.k9.fragment;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.fsck.k9.R;
/**
 Created by Kamil Rajtar on 05.11.17. */
class MessageSwipeReactions extends ItemTouchHelper.Callback{

	private final Resources resources;
	private final IOnDeleteListener onDeleteListener;

	MessageSwipeReactions(final Resources resources,final IOnDeleteListener onDeleteListener){
		this.onDeleteListener=onDeleteListener;
		this.resources=resources;
	}

	@Override
	public int getMovementFlags(final RecyclerView recyclerView,
								final RecyclerView.ViewHolder viewHolder){
		final int dragFlags=0;
		final int swipeFlags=ItemTouchHelper.END;
		return ItemTouchHelper.Callback.makeMovementFlags(dragFlags,swipeFlags);
	}

	@Override
	public boolean onMove(final RecyclerView recyclerView,final RecyclerView.ViewHolder viewHolder,
						  final RecyclerView.ViewHolder target){
		return false;
	}

	@Override
	public void onSwiped(final RecyclerView.ViewHolder viewHolder,final int direction){
		switch(direction){
			case ItemTouchHelper.END:
				onDeleteListener.onDelete(viewHolder.getAdapterPosition());
		}
	}

	public void onChildDraw(final Canvas canvas,final RecyclerView recyclerView,
							final RecyclerView.ViewHolder viewHolder,final float dX,final float dY,
							final int actionState,final boolean isCurrentlyActive){
		if(actionState==ItemTouchHelper.ACTION_STATE_SWIPE){
			// Get RecyclerView item from the MessageViewHolder
			final View itemView=viewHolder.itemView;
			final Bitmap icon;
			final RectF iconDest;

			final Paint paint=new Paint();
			paint.setColor(resources.getColor(R.color.swipe_delete));
			canvas.drawRect((float)itemView.getLeft(),(float)itemView.getTop(),dX,
					(float)itemView.getBottom(),paint);

			final float boxHeight=(float)itemView.getBottom()-(float)itemView.getTop();
			final float iconHeight=boxHeight/2;
			final float iconVerticalPadding=(boxHeight-iconHeight)/2;
			final float iconWidth=iconHeight;

			icon=drawableToBitmap(resources.getDrawable(R.drawable.ic_trash));
			iconDest=new RectF(0,itemView.getTop()+iconVerticalPadding,iconWidth,
					(float)itemView.getBottom()-iconVerticalPadding);
			canvas.drawBitmap(icon,null,iconDest,paint);

			super.onChildDraw(canvas,recyclerView,viewHolder,dX,dY,actionState,isCurrentlyActive);
		}
	}

	private Bitmap drawableToBitmap(final Drawable drawable){

		if(drawable instanceof BitmapDrawable){
			return ((BitmapDrawable)drawable).getBitmap();
		}

		final Bitmap bitmap=Bitmap.createBitmap(drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
		final Canvas canvas=new Canvas(bitmap);
		drawable.setBounds(0,0,canvas.getWidth(),canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}
}
