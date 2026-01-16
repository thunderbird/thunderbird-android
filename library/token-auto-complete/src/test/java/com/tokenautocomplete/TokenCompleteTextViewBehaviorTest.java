package com.tokenautocomplete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import android.graphics.Rect;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.RobolectricTestRunner;
import java.util.Arrays;
import java.util.Comparator;


@RunWith(RobolectricTestRunner.class)
public class TokenCompleteTextViewBehaviorTest {

    public static class ThemedTestActivity extends AppCompatActivity {
        @Override protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat);
            super.onCreate(savedInstanceState);
        }
    }

    static class ImeTrackingTokenView extends TokenCompleteTextView<String> {
        String lastClickedToken;
        int showImeCount;

        ImeTrackingTokenView(AppCompatActivity a) { super(a); setFocusableInTouchMode(true); }

        @Override protected View getViewForObject(String o) {
            TextView tv = new TextView(getContext());
            tv.setText(o);
            return tv;
        }

        @Override protected String defaultObject(String completionText) { return completionText; }

        @Override protected TokenImageSpan buildSpanForObject(String obj) {
            return new ClickSpan(getViewForObject(obj), obj);
        }

        class ClickSpan extends TokenImageSpan {
            ClickSpan(View d, String token) { super(d, token); }
            @Override public void onClick() {
                lastClickedToken = getToken();
                super.onClick();
            }
        }

        @Override public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
            super.onFocusChanged(hasFocus, direction, previous);
            if (hasFocus && shouldShowImeOnFocus()) showImeCount++;
        }

        void reset() { lastClickedToken = null; showImeCount = 0; }
    }

    static class Env {
        private static final int W = 2000, H = 400;

        final AppCompatActivity a = Robolectric.buildActivity(ThemedTestActivity.class).setup().get();
        final LinearLayout root = new LinearLayout(a);
        final EditText other = new EditText(a);
        final ImeTrackingTokenView v = new ImeTrackingTokenView(a);

        Env() {
            root.setOrientation(LinearLayout.VERTICAL);
            root.addView(other, lp());
            root.addView(v, lp());
            a.setContentView(root);
            layout();
        }

        private static LinearLayout.LayoutParams lp() {
            return new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        void idle() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }

        void layout() {
            int wSpec = View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY);
            int hSpec = View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY);
            root.measure(wSpec, hSpec);
            root.layout(0, 0, W, H);
            idle();
        }

        void focusOther() { other.requestFocus(); idle(); assertTrue(other.isFocused()); }
        void focusTokens() { v.requestFocus(); idle(); assertTrue(v.isFocused()); }

        void add(String... tokens) {
            for (String t : tokens) v.addObjectSync(t);
            layout();
        }

        void deleteTrailingSpace() {
            Editable t = v.getText();
            assertNotNull(t);
            if (t.length() > 0 && t.charAt(t.length() - 1) == ' ') t.delete(t.length() - 1, t.length());
            layout();
        }

        TokenCompleteTextView<?>.TokenImageSpan[] spans() {
            Editable t = v.getText();
            assertNotNull(t);
            TokenCompleteTextView<?>.TokenImageSpan[] s =
                    t.getSpans(0, t.length(), TokenCompleteTextView.TokenImageSpan.class);
            Arrays.sort(s, Comparator.comparingInt(t::getSpanStart));
            return s;
        }

        float yMidForSpan(TokenCompleteTextView<?>.TokenImageSpan span) {
            Editable t = v.getText();
            assertNotNull(t);
            int start = t.getSpanStart(span);
            assertTrue(start >= 0);
            int line = v.getLayout().getLineForOffset(start);
            return (v.getLayout().getLineTop(line) + v.getLayout().getLineBottom(line)) / 2f;
        }

        void tapText(float textX, float textY) {
            float xView = textX + v.getTotalPaddingLeft() - v.getScrollX();
            float yView = textY + v.getTotalPaddingTop() - v.getScrollY();
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, xView, yView, 0);
            MotionEvent up = MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, xView, yView, 0);
            v.dispatchTouchEvent(down);
            v.dispatchTouchEvent(up);
            down.recycle();
            up.recycle();
            idle();
        }

        void resetFromOther() { focusOther(); v.reset(); }

        void tapAndAssert(float x, float y, @Nullable String expectedToken, int expectedImeCount) {
            v.reset();
            tapText(x, y);
            assertEquals(expectedToken, v.lastClickedToken);
            assertEquals(expectedImeCount, v.showImeCount);
        }

        float emptyJustAfter(TokenCompleteTextView<?>.TokenImageSpan span, float y) {
            Editable t = v.getText();
            assertNotNull(t);
            float max = (v.getWidth() - v.getTotalPaddingLeft() - v.getTotalPaddingRight() - 1) + v.getScrollX();
            float x = Math.min(v.getLayout().getPrimaryHorizontal(t.getSpanEnd(span)) + 1f, max);
            for (float xi = x; xi <= max; xi += 1f) {
                if (v.hitTestTokenForTest(xi, y) == null) return xi;
            }
            fail("No empty space immediately after span");
            return 0f;
        }

        float inside(TokenCompleteTextView<?>.TokenImageSpan span, float y) {
            Editable t = v.getText();
            assertNotNull(t);
            float x = v.getLayout().getPrimaryHorizontal(t.getSpanStart(span)) + 1f;
            if (v.hitTestTokenForTest(x, y) != span) {
                for (float xi = x; xi <= x + 30f; xi += 1f) {
                    if (v.hitTestTokenForTest(xi, y) == span) return xi;
                }
                fail("Could not find inside point for span");
            }
            return x;
        }
    }

    @Test
    public void tapAfterLastToken_withoutTrailingSpace_doesNotOpenTokenMenu_andShowsIme() {
        Env e = new Env();
        e.add("one@example.com");
        e.deleteTrailingSpace();

        e.focusTokens();
        TokenCompleteTextView<?>.TokenImageSpan last = e.spans()[0];
        float y = e.yMidForSpan(last);
        float xEmpty = e.emptyJustAfter(last, y);

        e.resetFromOther();
        e.tapAndAssert(xEmpty, y, null, 1);
        assertTrue(e.v.isFocused());
    }

    @Test
    public void tapToken_edges_openCorrectTokenMenu() {
        Env e = new Env();
        e.add("one@example.com", "two@example.com", "three@example.com");
        e.focusTokens();

        TokenCompleteTextView<?>.TokenImageSpan[] spans = e.spans();
        assertEquals(3, spans.length);

        String[] expected = {"one@example.com", "two@example.com", "three@example.com"};
        for (int i = 0; i < spans.length; i++) {
            float y = e.yMidForSpan(spans[i]);

            Editable t = e.v.getText();
            assertNotNull(t);
            float xStart = e.v.getLayout().getPrimaryHorizontal(t.getSpanStart(spans[i]));

            float xLeft = -1;
            for (float xi = Math.max(0, xStart - 2); xi <= xStart + 50; xi += 1f) {
                if (e.v.hitTestTokenForTest(xi, y) == spans[i]) { xLeft = xi; break; }
            }
            assertTrue("Could not find left edge hit", xLeft >= 0);

            float xRight = xLeft;
            for (float xi = xLeft; xi <= xLeft + 300; xi += 1f) {
                if (e.v.hitTestTokenForTest(xi, y) == spans[i]) xRight = xi;
                else if (xi > xLeft + 10 && xRight > xLeft) break;
            }

            e.tapAndAssert(xLeft, y, expected[i], 0);
            e.tapAndAssert(xRight, y, expected[i], 0);
        }
    }

    @Test
    public void expandCollapsedField_cursorDoesNotAppearAtStart_andEndsAtEnd() {
        Env e = new Env();
        e.add("one@example.com", "two@example.com", "three@example.com");
        e.v.getText().append("tail");
        e.layout();

        e.v.setCursorVisible(true);

        e.v.setHiddenContentForTest(new SpannableStringBuilder(e.v.getText()));
        e.v.setText("one@example.com, +2");
        e.layout();

        e.v.performCollapse(true);

        assertFalse(e.v.isCursorVisible());
        e.idle();

        assertEquals(e.v.getText().length(), e.v.getSelectionStart()); // cursor at the end
        assertTrue(e.v.isCursorVisible());
    }

    @Test
    public void tokenTaps_neverRequestIme_emptyTap_requestsIme() {
        Env e = new Env();
        e.add("one@example.com", "two@example.com");

        e.focusTokens();
        TokenCompleteTextView<?>.TokenImageSpan[] spans = e.spans();
        TokenCompleteTextView<?>.TokenImageSpan first = spans[0];
        TokenCompleteTextView<?>.TokenImageSpan last  = spans[spans.length - 1];

        float yFirst = e.yMidForSpan(first);
        float xToken = e.inside(first, yFirst);

        float yLast = e.yMidForSpan(last);
        float xEmpty = e.emptyJustAfter(last, yLast);
        assertNull(e.v.hitTestTokenForTest(xEmpty, yLast)); // empty

        e.resetFromOther();
        e.tapAndAssert(xToken, yFirst, "one@example.com", 0);

        for (int i = 0; i < 3; i++) {
            e.tapAndAssert(xToken, yFirst, "one@example.com", 0);
        }

        e.resetFromOther();
        e.tapAndAssert(xEmpty, yLast, null, 1);
    }
}
