package com.fsck.k9.view;

import com.fsck.k9.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Class representing a LinearLayout that can fold and hide it's content when
 * pressed To use just add the following to your xml layout
 * <org.sufficientlysecure.keychain.ui.widget.FoldableLinearLayout
 * android:layout_width="wrap_content" android:layout_height="wrap_content"
 * custom:foldedLabel="@string/TEXT_TO_DISPLAY_WHEN_FOLDED"
 * custom:unFoldedLabel="@string/TEXT_TO_DISPLAY_WHEN_UNFOLDED"
 * custom:foldedIcon="ICON_NAME_FROM_FontAwesomeText_TO_USE_WHEN_FOLDED"
 * custom:unFoldedIcon="ICON_NAME_FROM_FontAwesomeText_TO_USE_WHEN_UNFOLDED">
 * <include layout="@layout/ELEMENTS_TO_BE_FOLDED"/>
 * </org.sufficientlysecure.keychain.ui.widget.FoldableLinearLayout>
 */
public class FoldableLinearLayout extends LinearLayout {
    private ImageButton mFoldableIcon;
    private boolean mFolded;
    private boolean mHasMigrated = false;
    private Integer mShortAnimationDuration = null;
    private TextView mFoldableTextView = null;
    private LinearLayout mFoldableContainer = null;
    private View mFoldableLayout = null;
    private String mFoldedLabel;
    private String mUnFoldedLabel;

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
        mFoldableIcon = (ImageButton) mFoldableLayout.findViewById(R.id.foldableIcon);
        mFoldableIcon.setImageResource(R.drawable.ic_action_expand);
        mFoldableTextView = (TextView) mFoldableLayout.findViewById(R.id.foldableText);
        mFoldableTextView.setText(mFoldedLabel);
        // retrieve and cache the system's short animation time
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        LinearLayout foldableControl = (LinearLayout) mFoldableLayout
                .findViewById(R.id.foldableControl);
        foldableControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFolded = !mFolded;
                if (mFolded) {
                    mFoldableIcon.setImageResource(R.drawable.ic_action_collapse);
                    mFoldableContainer.setVisibility(View.VISIBLE);
                    AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                    animation.setDuration(mShortAnimationDuration);
                    mFoldableContainer.startAnimation(animation);
                    mFoldableTextView.setText(mUnFoldedLabel);
                } else {
                    mFoldableIcon.setImageResource(R.drawable.ic_action_expand);
                    AlphaAnimation animation = new AlphaAnimation(1f, 0f);
                    animation.setDuration(mShortAnimationDuration);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            // making sure that at the end the container is
                            // completely removed from view
                            mFoldableContainer.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    mFoldableContainer.startAnimation(animation);
                    mFoldableTextView.setText(mFoldedLabel);
                }
            }
        });
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
