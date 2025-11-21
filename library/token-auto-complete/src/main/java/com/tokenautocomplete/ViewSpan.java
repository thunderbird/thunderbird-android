package com.tokenautocomplete;

import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.view.ViewGroup;

/**
 * Span that holds a view it draws when rendering
 *
 * Created on 2/3/15.
 * @author mgod
 */
public class ViewSpan extends ReplacementSpan {
    protected View view;
    private ViewSpan.Layout layout;
    private int cachedMaxWidth = -1;

    @SuppressWarnings("WeakerAccess")
    public ViewSpan(View view, ViewSpan.Layout layout) {
        super();
        this.layout = layout;
        this.view = view;
        this.view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                             ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void prepView() {
        if (layout.getMaxViewSpanWidth() != cachedMaxWidth || view.isLayoutRequested()) {
            cachedMaxWidth = layout.getMaxViewSpanWidth();

            int spec = View.MeasureSpec.AT_MOST;
            if (cachedMaxWidth == 0) {
                //If the width is 0, allow the view to choose it's own content size
                spec = View.MeasureSpec.UNSPECIFIED;
            }
            int widthSpec = View.MeasureSpec.makeMeasureSpec(cachedMaxWidth, spec);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            view.measure(widthSpec, heightSpec);
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, @IntRange(from = 0) int start,
                     @IntRange(from = 0) int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        prepView();

        canvas.save();
        canvas.translate(x, top);
        view.draw(canvas);
        canvas.restore();
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence charSequence, @IntRange(from = 0) int start,
                       @IntRange(from = 0) int end, @Nullable Paint.FontMetricsInt fontMetricsInt) {
        prepView();

        if (fontMetricsInt != null) {
            //We need to make sure the layout allots enough space for the view
            int height = view.getMeasuredHeight();

            int adjustedBaseline = view.getBaseline();
            //-1 means the view doesn't support baseline alignment, so align bottom to font baseline
            if (adjustedBaseline == -1) {
                adjustedBaseline = height;
            }
            fontMetricsInt.ascent = fontMetricsInt.top = -adjustedBaseline;
            fontMetricsInt.descent = fontMetricsInt.bottom = height - adjustedBaseline;
        }

        return view.getRight();
    }

    public interface Layout {
        int getMaxViewSpanWidth();
    }
}
