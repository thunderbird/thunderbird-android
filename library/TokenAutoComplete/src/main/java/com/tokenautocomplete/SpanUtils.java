package com.tokenautocomplete;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;

public class SpanUtils {

    private static class EllipsizeCallback implements TextUtils.EllipsizeCallback {
        int start = 0;
        int end = 0;

        @Override
        public void ellipsized(int ellipsedStart, int ellipsedEnd) {
            start = ellipsedStart;
            end = ellipsedEnd;
        }
    }

    @Nullable
    public static Spanned ellipsizeWithSpans(@Nullable CharSequence prefix, @Nullable CountSpan countSpan,
                                           int tokenCount, @NonNull TextPaint paint,
                                           @NonNull CharSequence originalText, float maxWidth) {

        float countWidth = 0;
        if (countSpan != null) {
            //Assume the largest possible number of items for measurement
            countSpan.setCount(tokenCount);
            countWidth = countSpan.getCountTextWidthForPaint(paint);
        }

        EllipsizeCallback ellipsizeCallback = new EllipsizeCallback();
        CharSequence tempEllipsized = TextUtils.ellipsize(originalText, paint, maxWidth - countWidth,
                TextUtils.TruncateAt.END, false, ellipsizeCallback);
        SpannableStringBuilder ellipsized = new SpannableStringBuilder(tempEllipsized);
        if (tempEllipsized instanceof Spanned) {
            TextUtils.copySpansFrom((Spanned)tempEllipsized, 0, tempEllipsized.length(), Object.class, ellipsized, 0);
        }

        if (prefix != null && prefix.length() > ellipsizeCallback.start) {
            //We ellipsized part of the prefix, so put it back
            ellipsized.replace(0, ellipsizeCallback.start, prefix);
            ellipsizeCallback.end = ellipsizeCallback.end + prefix.length() - ellipsizeCallback.start;
            ellipsizeCallback.start = prefix.length();
        }

        if (ellipsizeCallback.start != ellipsizeCallback.end) {

            if (countSpan != null) {
                int visibleCount = ellipsized.getSpans(0, ellipsized.length(), TokenCompleteTextView.TokenImageSpan.class).length;
                countSpan.setCount(tokenCount - visibleCount);
                ellipsized.replace(ellipsizeCallback.start, ellipsized.length(), countSpan.getCountText());
                ellipsized.setSpan(countSpan, ellipsizeCallback.start, ellipsized.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return ellipsized;
        }
        //No ellipses necessary
        return null;
    }
}
