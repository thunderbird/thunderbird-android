package com.tokenautocomplete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import android.content.Context;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(RobolectricTestRunner.class)
public class TokenCompleteTextViewStateRestoreTest {

    static class TestTokenView extends TokenCompleteTextView<String> {
        TestTokenView(Context context) { super(context); }

        @Override protected View getViewForObject(String object) {
            TextView tv = new TextView(getContext());
            tv.setText(object);
            return tv;
        }

        @Override protected String defaultObject(String completionText) {
            return completionText;
        }

        String visibleText() {
            return getText() == null ? "" : getText().toString();
        }

        String fullText() {
            CharSequence c = getContentText();
            return c == null ? "" : c.toString();
        }
    }

    private static Context themedContext() {
        Context base = ApplicationProvider.getApplicationContext();
        return new ContextThemeWrapper(base, androidx.appcompat.R.style.Theme_AppCompat);
    }

    private static TestTokenView newView(Context context, boolean allowCollapse) {
        TestTokenView v = new TestTokenView(context);
        v.allowCollapse(allowCollapse);
        return v;
    }

    private static TestTokenView roundTrip(Context context, TestTokenView before) {
        Parcelable state = before.onSaveInstanceState();
        TestTokenView after = newView(context, true);
        after.onRestoreInstanceState(state);
        return after;
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private static int countPlusIndicators(String s) {
        Matcher m = Pattern.compile("\\+\\d+").matcher(s);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    @Test
    public void restore_singleToken_plusTail_noDuplication_fullTextPreserved() {
        Context context = themedContext();

        TestTokenView before = newView(context, false);
        before.addObjectSync("one@example.com");
        before.getText().append("tail");

        String expectedFull = before.fullText();
        TestTokenView after = roundTrip(context, before);

        assertEquals(expectedFull, after.fullText());
        assertEquals(List.of("one@example.com"), after.getObjects());
        assertEquals(1, countOccurrences(after.fullText(), "one@example.com"));
        assertTrue(after.fullText().contains("tail"));
    }

    @Test
    public void restore_twoTokens_plusTail_noDuplication_andOrderPreserved() {
        Context context = themedContext();

        TestTokenView before = newView(context, false);
        before.addObjectSync("one@example.com");
        before.addObjectSync("two@example.com");
        before.getText().append("tail");

        String expectedFull = before.fullText();
        TestTokenView after = roundTrip(context, before);

        assertEquals(expectedFull, after.fullText());
        assertEquals(List.of("one@example.com", "two@example.com"), after.getObjects());
        assertEquals(1, countOccurrences(after.fullText(), "one@example.com"));
        assertEquals(1, countOccurrences(after.fullText(), "two@example.com"));
        assertTrue(after.fullText().contains("tail"));
    }

    @Test
    public void restore_fromCollapsedState_doesNotPersistPlusIndicator_andRestoresFullContent() throws Exception {
        Context context = themedContext();

        TestTokenView before = newView(context, true);
        before.addObjectSync("one@example.com");
        before.addObjectSync("two@example.com");
        before.addObjectSync("three@example.com");
        before.getText().append("tail");

        String expectedExpanded = before.fullText();
        assertEquals(0, countPlusIndicators(expectedExpanded)); // expanded content must never contain "+x"

        // Deterministically simulate "collapsed" state
        Field hiddenField = TokenCompleteTextView.class.getDeclaredField("hiddenContent");
        hiddenField.setAccessible(true);

        assertTrue("Test relies on TextView using SpannableStringBuilder internally",
            before.getText() instanceof SpannableStringBuilder);
        hiddenField.set(before, before.getText());

        // Visible/collapsed representation
        before.setText("one@example.com, +2");

        assertEquals(1, countPlusIndicators(before.visibleText()));
        assertEquals(expectedExpanded, before.fullText()); // full text comes from hiddenContent

        Parcelable state = before.onSaveInstanceState();
        TestTokenView after = newView(context, true);
        after.onRestoreInstanceState(state);

        // Restored full text must not contain "+x"
        assertEquals(expectedExpanded, after.fullText());
        assertEquals(0, countPlusIndicators(after.fullText()));

        assertEquals(List.of("one@example.com", "two@example.com", "three@example.com"), after.getObjects());
        assertTrue(after.fullText().contains("tail"));

        // No duplicated raw token text
        assertEquals(1, countOccurrences(after.fullText(), "one@example.com"));
        assertEquals(1, countOccurrences(after.fullText(), "two@example.com"));
        assertEquals(1, countOccurrences(after.fullText(), "three@example.com"));
    }
}
