package app.k9mail.ui.utils.linearlayoutmanager;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

// Source: https://github.com/SchildiChat/SchildiChat-android/blob/a321d6a79a1dc93bcfea442390e49c557285eabe/vector/src/main/java/de/spiritcroc/recyclerview/widget/LayoutManager.java
/**
 * Exposing/replicating some internal functions from RecylerView.LayoutManager
 */
public abstract class LayoutManager extends RecyclerView.LayoutManager {


   /*
    * Exposed things from RecyclerView.java
    */

   /**
    * The callback used for retrieving information about a RecyclerView and its children in the
    * horizontal direction.
    */
   private final ViewBoundsCheck.Callback mHorizontalBoundCheckCallback =
           new ViewBoundsCheck.Callback() {
              @Override
              public View getChildAt(int index) {
                 return LayoutManager.this.getChildAt(index);
              }

              @Override
              public int getParentStart() {
                 return LayoutManager.this.getPaddingLeft();
              }

              @Override
              public int getParentEnd() {
                 return LayoutManager.this.getWidth() - LayoutManager.this.getPaddingRight();
              }

              @Override
              public int getChildStart(View view) {
                 final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                         view.getLayoutParams();
                 return LayoutManager.this.getDecoratedLeft(view) - params.leftMargin;
              }

              @Override
              public int getChildEnd(View view) {
                 final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                         view.getLayoutParams();
                 return LayoutManager.this.getDecoratedRight(view) + params.rightMargin;
              }
           };

   /**
    * The callback used for retrieving information about a RecyclerView and its children in the
    * vertical direction.
    */
   private final ViewBoundsCheck.Callback mVerticalBoundCheckCallback =
           new ViewBoundsCheck.Callback() {
              @Override
              public View getChildAt(int index) {
                 return LayoutManager.this.getChildAt(index);
              }

              @Override
              public int getParentStart() {
                 return LayoutManager.this.getPaddingTop();
              }

              @Override
              public int getParentEnd() {
                 return LayoutManager.this.getHeight()
                         - LayoutManager.this.getPaddingBottom();
              }

              @Override
              public int getChildStart(View view) {
                 final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                         view.getLayoutParams();
                 return LayoutManager.this.getDecoratedTop(view) - params.topMargin;
              }

              @Override
              public int getChildEnd(View view) {
                 final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                         view.getLayoutParams();
                 return LayoutManager.this.getDecoratedBottom(view) + params.bottomMargin;
              }
           };

   /**
    * Utility objects used to check the boundaries of children against their parent
    * RecyclerView.
    *
    * @see #isViewPartiallyVisible(View, boolean, boolean),
    * {@link LinearLayoutManager#findOneVisibleChild(int, int, boolean, boolean)},
    * and {@link LinearLayoutManager#findOnePartiallyOrCompletelyInvisibleChild(int, int)}.
    */
   ViewBoundsCheck mHorizontalBoundCheck = new ViewBoundsCheck(mHorizontalBoundCheckCallback);
   ViewBoundsCheck mVerticalBoundCheck = new ViewBoundsCheck(mVerticalBoundCheckCallback);

}
