package com.fsck.k9.view;

import com.fsck.k9.R;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Class representing a LinearLayout that can fold and hide it's content when
 * pressed To use just add the following to your xml layout
 * <com.fsck.k9.view.FoldableLinearLayout
 * android:layout_width="wrap_content" android:layout_height="wrap_content"
 * custom:foldedLabel="@string/TEXT_TO_DISPLAY_WHEN_FOLDED"
 * custom:unFoldedLabel="@string/TEXT_TO_DISPLAY_WHEN_UNFOLDED">
 * <include layout="@layout/ELEMENTS_TO_BE_FOLDED"/>
 * </com.fsck.k9.view.FoldableLinearLayout>
 */
public class FoldableLinearLayout extends LinearLayout {

    private ImageView mFoldableIcon;

    // Start with the view folded
    private boolean mIsFolded = true;
    private boolean mHasMigrated = false;
    private Integer mShortAnimationDuration = null;
    private TextView mFoldableTextView = null;
    private LinearLayout mFoldableContainer = null;
    private View mFoldableLayout = null;
    private String mFoldedLabel;
    private String mUnFoldedLabel;
    private int mIconActionCollapseId;
    private int mIconActionExpandId;

    public FoldableLinearLayout(Context context) {
        super(context);
        processAttributes(context, null);
    }

    public FoldableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        processAttributes(context, attrs);
    }

    public FoldableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        processAttributes(context, attrs);
    }

    /**
     * Load given attributes to inner variables,
     * 
     * @param context
     * @param attrs
     */
    private void processAttributes(Context context, AttributeSet attrs) {
        Theme theme = context.getTheme();
        TypedValue outValue = new TypedValue();
        boolean found = theme.resolveAttribute(R.attr.iconActionCollapse, outValue, true);
        if (found) {
            mIconActionCollapseId = outValue.resourceId;
        }
        found = theme.resolveAttribute(R.attr.iconActionExpand, outValue, true);
        if (found) {
            mIconActionExpandId = outValue.resourceId;
        }
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.FoldableLinearLayout, 0, 0);
            mFoldedLabel = a.getString(R.styleable.FoldableLinearLayout_foldedLabel);
            mUnFoldedLabel = a.getString(R.styleable.FoldableLinearLayout_unFoldedLabel);
            a.recycle();
        }
        // If any attribute isn't found then set a default one
        mFoldedLabel = (mFoldedLabel == null) ? "No text!" : mFoldedLabel;
        mUnFoldedLabel = (mUnFoldedLabel == null) ? "No text!" : mUnFoldedLabel;
    }

    @Override
    protected void onFinishInflate() {
        // if the migration has already happened
        // there is no need to move any children
        if (!mHasMigrated) {
            migrateChildrenToContainer();
            mHasMigrated = true;
        }
        initialiseInnerViews();
        super.onFinishInflate();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mFolded = mIsFolded;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            mIsFolded = savedState.mFolded;
            updateFoldedState(mIsFolded, false);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    static class SavedState extends BaseSavedState {

        static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<FoldableLinearLayout.SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private boolean mFolded;

        private SavedState(Parcel parcel) {
            super(parcel);
            mFolded = (parcel.readInt() == 1);
        }

        private SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mFolded ? 1 : 0);
        }
    }

    /**
     * Migrates Child views as declared in xml to the inner foldableContainer
     */
    private void migrateChildrenToContainer() {
        // Collect children of FoldableLinearLayout as declared in XML
        int childNum = getChildCount();
        View[] children = new View[childNum];
        for (int i = 0; i < childNum; i++) {
            children[i] = getChildAt(i);
        }
        if (children[0].getId() == R.id.foldableControl) {
        }
        // remove all of them from FoldableLinearLayout
        detachAllViewsFromParent();
        // Inflate the inner foldable_linearlayout.xml
        LayoutInflater inflator = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mFoldableLayout = inflator.inflate(R.layout.foldable_linearlayout, this, true);
        mFoldableContainer = (LinearLayout) mFoldableLayout.findViewById(R.id.foldableContainer);
        // Push previously collected children into foldableContainer.
        for (int i = 0; i < childNum; i++) {
            addView(children[i]);
        }
    }

    private void initialiseInnerViews() {
        mFoldableIcon = (ImageView) mFoldableLayout.findViewById(R.id.foldableIcon);
        mFoldableTextView = (TextView) mFoldableLayout.findViewById(R.id.foldableText);
        mFoldableTextView.setText(mFoldedLabel);
        // retrieve and cache the system's short animation time
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        LinearLayout foldableControl = (LinearLayout) mFoldableLayout
                .findViewById(R.id.foldableControl);
        foldableControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsFolded = !mIsFolded;
                updateFoldedState(mIsFolded, true);
            }
        });
    }

    protected void updateFoldedState(boolean newStateIsFolded, boolean animate) {
        if (newStateIsFolded) {
            mFoldableIcon.setImageResource(mIconActionExpandId);
            if (animate) {
                AlphaAnimation animation = new AlphaAnimation(1f, 0f);
                animation.setDuration(mShortAnimationDuration);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        /*
                         * Make sure that at the end the container is
                         * completely invisible. GONE is not used in
                         * order to prevent parent views from jumping
                         * around as they re-center themselves
                         * vertically.
                         */
                        mFoldableContainer.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                mFoldableContainer.startAnimation(animation);
            } else {
                mFoldableContainer.setVisibility(View.INVISIBLE);
            }
            mFoldableTextView.setText(mFoldedLabel);
        } else {
            mFoldableIcon.setImageResource(mIconActionCollapseId);
            mFoldableContainer.setVisibility(View.VISIBLE);
            if (animate) {
                AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                animation.setDuration(mShortAnimationDuration);
                mFoldableContainer.startAnimation(animation);
            }
            mFoldableTextView.setText(mUnFoldedLabel);
        }
    }

    /**
     * Adds provided child view to foldableContainer View
     * 
     * @param child
     */
    @Override
    public void addView(View child) {
        if (mFoldableContainer != null) {
            mFoldableContainer.addView(child);
        }
    }
}
