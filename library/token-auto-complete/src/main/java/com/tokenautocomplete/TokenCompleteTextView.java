package com.tokenautocomplete;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GMail style auto complete view with easy token customization
 * override getViewForObject to provide your token view
 * <br>
 * Created by mgod on 9/12/13.
 *
 * @author mgod
 */
public abstract class TokenCompleteTextView<T> extends AppCompatAutoCompleteTextView
        implements TextView.OnEditorActionListener, ViewSpan.Layout {
    //Logging
    public static final String TAG = "TokenAutoComplete";

    private Tokenizer tokenizer;
    private T selectedObject;
    private TokenListener<T> listener;
    private TokenSpanWatcher spanWatcher;
    private TokenTextWatcher textWatcher;
    private CountSpan countSpan;
    private @Nullable SpannableStringBuilder hiddenContent;
    private Layout lastLayout = null;
    private boolean initialized = false;
    private boolean performBestGuess = true;
    private boolean savingState = false;
    private boolean shouldFocusNext = false;
    private boolean allowCollapse = true;
    private boolean internalEditInProgress = false;

    @Nullable private TokenImageSpan pressedTokenSpan;
    // When focus is gained due to tapping a token, don't auto-show the IME (avoids keyboard flicker).
    private boolean suppressImeOnNextFocus;

    private int tokenLimit = -1;

    private transient String lastCompletionText = null;

    /**
     * Add the TextChangedListeners
     */
    protected void addListeners() {
        Editable text = getText();
        if (text != null) {
            text.setSpan(spanWatcher, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            addTextChangedListener(textWatcher);
        }
    }

    /**
     * Remove the TextChangedListeners
     */
    protected void removeListeners() {
        Editable text = getText();
        if (text != null) {
            TokenSpanWatcher[] spanWatchers = text.getSpans(0, text.length(), TokenSpanWatcher.class);
            for (TokenSpanWatcher watcher : spanWatchers) {
                text.removeSpan(watcher);
            }
            removeTextChangedListener(textWatcher);
        }
    }

    /**
     * Initialise the variables and various listeners
     */
    private void init() {
        if (initialized) return;

        // Initialise variables
        setTokenizer(new CharacterTokenizer(Arrays.asList(',', ';'), ","));
        Editable text = getText();
        assert null != text;
        spanWatcher = new TokenSpanWatcher();
        textWatcher = new TokenTextWatcher();
        hiddenContent = null;
        countSpan = new CountSpan();

        // Initialise TextChangedListeners
        addListeners();

        setTextIsSelectable(false);
        setLongClickable(false);

        //In theory, get the soft keyboard to not supply suggestions. very unreliable
        setInputType(getInputType() |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        setHorizontallyScrolling(false);

        // Listen to IME action keys
        setOnEditorActionListener(this);

        // Initialise the text filter (listens for the split chars)
        setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int destinationStart, int destinationEnd) {
                if (internalEditInProgress) {
                    return null;
                }

                // Token limit check
                if (tokenLimit != -1 && getObjects().size() == tokenLimit) {
                    return "";
                }

                //Detect split characters, remove them and complete the current token instead
                // We only want to handle the case where the user inputs a single split character here
                if (source.length() == 1 && tokenizer.containsTokenTerminator(source)) {
                    performCompletion();
                    return "";
                }

                return null;
            }
        }});

        initialized = true;
    }

    public TokenCompleteTextView(Context context) {
        super(context);
        init();
    }

    public TokenCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TokenCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        Filter filter = getFilter();
        if (filter != null) {
            filter.filter(currentCompletionText(), this);
        }
    }

    public void setTokenizer(Tokenizer t) {
        tokenizer = t;
    }

    /**
     * Set the listener that will be notified of changes in the Token list
     *
     * @param l The TokenListener
     */
    public void setTokenListener(TokenListener<T> l) {
        listener = l;
    }

    /**
     * Override if you want to prevent a token from being added. Defaults to false.
     * @param token the token to check
     * @return true if the token should not be added, false if it's ok to add it.
     */
    public boolean shouldIgnoreToken(@SuppressWarnings("unused") T token) {
        return false;
    }

    /**
     * Override if you want to prevent a token from being removed. Defaults to true.
     * @param token the token to check
     * @return false if the token should not be removed, true if it's ok to remove it.
     */
    public boolean isTokenRemovable(@SuppressWarnings("unused") T token) {
        return true;
    }

    /**
     * Get the list of Tokens
     *
     * @return List of tokens
     */
    public List<T> getObjects() {
        ArrayList<T>objects = new ArrayList<>();
        Editable text = getText();
        if (hiddenContent != null) {
            text = hiddenContent;
        }
        for (TokenImageSpan span: text.getSpans(0, text.length(), TokenImageSpan.class)) {
            objects.add(span.getToken());
        }
        return objects;
    }

    /**
     * Get the content entered in the text field, including hidden text when ellipsized
     *
     * @return CharSequence of the entered content
     */
    public CharSequence getContentText() {
        if (hiddenContent != null) {
            return hiddenContent;
        } else {
            return getText();
        }
    }

    /**
     * Set whether we try to guess an entry from the autocomplete spinner or just use the
     * defaultObject implementation for inline token completion.
     *
     * @param guess true to enable guessing
     */
    public void performBestGuess(boolean guess) {
        performBestGuess = guess;
    }

    /**
     * Set whether the view should collapse to a single line when it loses focus.
     *
     * @param allowCollapse true if it should collapse
     */
    public void allowCollapse(boolean allowCollapse) {
        this.allowCollapse = allowCollapse;
    }

    /**
     * Set a number of tokens limit.
     *
     * @param tokenLimit The number of tokens permitted. -1 value disables limit.
     */
    @SuppressWarnings("unused")
    public void setTokenLimit(int tokenLimit) {
        this.tokenLimit = tokenLimit;
    }

    /**
     * A token view for the object
     *
     * @param object the object selected by the user from the list
     * @return a view to display a token in the text field for the object
     */
    abstract protected View getViewForObject(T object);

    /**
     * Provides a default completion when the user hits , and there is no item in the completion
     * list
     *
     * @param completionText the current text we are completing against
     * @return a best guess for what the user meant to complete or null if you don't want a guess
     */
    abstract protected T defaultObject(String completionText);

    /**
     * Correctly build accessibility string for token contents
     *
     * This seems to be a hidden API, but there doesn't seem to be another reasonable way
     * @return custom string for accessibility
     */
    @SuppressWarnings("unused")
    public CharSequence getTextForAccessibility() {
        if (getObjects().size() == 0) {
            return getText();
        }

        SpannableStringBuilder description = new SpannableStringBuilder();
        Editable text = getText();
        int selectionStart = -1;
        int selectionEnd = -1;
        int i;
        //Need to take the existing tet buffer and
        // - replace all tokens with a decent string representation of the object
        // - set the selection span to the corresponding location in the new CharSequence
        for (i = 0; i < text.length(); ++i) {
            //See if this is where we should start the selection
            int origSelectionStart = Selection.getSelectionStart(text);
            if (i == origSelectionStart) {
                selectionStart = description.length();
            }
            int origSelectionEnd = Selection.getSelectionEnd(text);
            if (i == origSelectionEnd) {
                selectionEnd = description.length();
            }

            //Replace token spans
            TokenImageSpan[] tokens = text.getSpans(i, i, TokenImageSpan.class);
            if (tokens.length > 0) {
                TokenImageSpan token = tokens[0];
                description = description.append(tokenizer.wrapTokenValue(token.getToken().toString()));
                i = text.getSpanEnd(token);
                continue;
            }

            description = description.append(text.subSequence(i, i + 1));
        }

        int origSelectionStart = Selection.getSelectionStart(text);
        if (i == origSelectionStart) {
            selectionStart = description.length();
        }
        int origSelectionEnd = Selection.getSelectionEnd(text);
        if (i == origSelectionEnd) {
            selectionEnd = description.length();
        }

        if (selectionStart >= 0 && selectionEnd >= 0) {
            Selection.setSelection(description, selectionStart, selectionEnd);
        }

        return description;
    }

    /**
     * Clear the completion text only.
     */
    @SuppressWarnings("unused")
    public void clearCompletionText() {
        //Respect currentCompletionText in case hint is visible or if other checks are added.
        if (currentCompletionText().length() == 0){
            return;
        }

        Range currentRange = getCurrentCandidateTokenRange();
        internalEditInProgress = true;
        getText().delete(currentRange.start, currentRange.end);
        internalEditInProgress = false;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            CharSequence text = getTextForAccessibility();
            event.setFromIndex(Selection.getSelectionStart(text));
            event.setToIndex(Selection.getSelectionEnd(text));
            event.setItemCount(text.length());
        }
    }

    private Range getCurrentCandidateTokenRange() {
        Editable editable = getText();
        int cursorEndPosition = getSelectionEnd();
        int candidateStringStart = 0;
        int candidateStringEnd = editable.length();

        //We want to find the largest string that contains the selection end that is not already tokenized
        TokenImageSpan[] spans = editable.getSpans(0, editable.length(), TokenImageSpan.class);
        for (TokenImageSpan span : spans) {
            int spanEnd = editable.getSpanEnd(span);
            if (candidateStringStart < spanEnd && cursorEndPosition >= spanEnd) {
                candidateStringStart = spanEnd;
            }
            int spanStart = editable.getSpanStart(span);
            if (candidateStringEnd > spanStart && cursorEndPosition <= spanEnd) {
                candidateStringEnd = spanStart;
            }
        }
        if (candidateStringEnd < candidateStringStart) {
            return new Range(cursorEndPosition, cursorEndPosition);
        }
        return new Range(candidateStringStart, candidateStringEnd);
    }

    /**
     * Override if you need custom logic to provide a sting representation of a token
     * @param token the token to convert
     * @return the string representation of the token. Defaults to {@link Object#toString()}
     */
    protected CharSequence tokenToString(T token) {
        return token.toString();
    }

    protected String currentCompletionText() {
        Editable editable = getText();
        Range currentRange = getCurrentCandidateTokenRange();

        String result = TextUtils.substring(editable, currentRange.start, currentRange.end).trim();
        Log.d(TAG, "Current completion text: " + result);
        return result;
    }

    protected float maxTextWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    @Override
    public int getMaxViewSpanWidth() {
        return (int)maxTextWidth();
    }

    public void redrawTokens() {
        // There's no straight-forward way to convince the widget to redraw the text and spans. We trigger a redraw by
        // making an invisible change (either adding or removing a dummy span).

        Editable text = getText();
        if (text == null) return;

        int textLength = text.length();
        DummySpan[] dummySpans = text.getSpans(0, textLength, DummySpan.class);
        if (dummySpans.length > 0) {
            text.removeSpan(DummySpan.INSTANCE);
        } else {
            text.setSpan(DummySpan.INSTANCE, 0, textLength, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    @Override
    public boolean enoughToFilter() {
        if (tokenizer == null) {
            return false;
        }

        int cursorPosition = getSelectionEnd();

        if (cursorPosition < 0) {
            return false;
        }

        Range currentCandidateRange = getCurrentCandidateTokenRange();

        //Don't allow 0 length entries to filter
        return currentCandidateRange.length() >= Math.max(getThreshold(), 1);
    }

    @Override
    public void performCompletion() {
        if ((getAdapter() == null || getListSelection() == ListView.INVALID_POSITION) && enoughToFilter()) {
            Object bestGuess;
            if (getAdapter() != null && getAdapter().getCount() > 0 && performBestGuess) {
                bestGuess = getAdapter().getItem(0);
            } else {
                bestGuess = defaultObject(currentCompletionText());
            }
            replaceText(convertSelectionToString(bestGuess));
        } else {
            super.performCompletion();
        }
    }

    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        InputConnection superConn = super.onCreateInputConnection(outAttrs);
        if (superConn != null) {
            TokenInputConnection conn = new TokenInputConnection(superConn, true);
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
            outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
            return conn;
        } else {
            return null;
        }
    }

    /**
     * Create a token and hide the keyboard when the user sends the DONE IME action
     * Use IME_NEXT if you want to create a token and go to the next field
     */
    private void handleDone() {
        // Attempt to complete the current token token
        performCompletion();

        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        boolean handled = super.onKeyUp(keyCode, event);
        if (shouldFocusNext) {
            shouldFocusNext = false;
            handleDone();
        }
        return handled;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_TAB:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.hasNoModifiers()) {
                    shouldFocusNext = true;
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                handled = !canDeleteSelection(1);
                break;
        }

        return handled || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_DONE) {
            handleDone();
            return true;
        }
        return false;
    }

    protected final void suppressImeOnNextFocus() {
        suppressImeOnNextFocus = true;
    }

    protected final boolean shouldShowImeOnFocus() {
        final boolean suppressed = suppressImeOnNextFocus;
        suppressImeOnNextFocus = false;
        return !suppressed;
    }

    @VisibleForTesting
    @Nullable
    TokenImageSpan hitTestTokenForTest(float textX, float textY) {
        Editable text = getText();
        Layout layout = getLayout();
        if (text == null || layout == null) return null;
        return findTokenSpanUnderTouch(text, layout, textX, textY);
    }

    @VisibleForTesting
    void setHiddenContentForTest(@Nullable SpannableStringBuilder content) {
        hiddenContent = content;
    }

    @Nullable
    private TokenImageSpan findTokenSpanUnderTouch(@NonNull Editable text, @NonNull Layout layout, float x, float y) {
        if (x < 0f || y < 0f || y > layout.getHeight()) return null;

        final int line = layout.getLineForVertical((int) y);

        // Don't treat taps in the line's empty gutter as token taps.
        final float leftEdge  = Math.min(layout.getLineLeft(line),  layout.getLineRight(line));
        final float rightEdge = Math.max(layout.getLineLeft(line),  layout.getLineRight(line));
        if (x < leftEdge || x > rightEdge) return null;

        final int offset = layout.getOffsetForHorizontal(line, x);
        final int qs = Math.max(0, offset - 1);
        final int qe = Math.min(text.length(), offset + 1);

        TokenImageSpan best = null;
        int bestStart = Integer.MIN_VALUE;

        for (TokenImageSpan s : text.getSpans(qs, qe, TokenImageSpan.class)) {
            final int start = text.getSpanStart(s);
            if (start > bestStart && isTouchInsideSpan(text, layout, s, x, y)) {
                // Near boundaries multiple spans can be returned; prefer the span with the greatest start index.
                bestStart = start;
                best = s;
            }
        }
        return best;
    }

    private boolean isTouchInsideSpan(@NonNull Editable text, @NonNull Layout layout,
        @NonNull TokenImageSpan span, float x, float y) {
        final int start = text.getSpanStart(span);
        final int end   = text.getSpanEnd(span);
        if (start < 0 || end <= start) return false;

        final int startLine = layout.getLineForOffset(start);
        final int endLine   = layout.getLineForOffset(end);

        for (int line = startLine; line <= endLine; line++) {
            final int top = layout.getLineTop(line);
            final int bottom = layout.getLineBottom(line);
            if (y < top || y > bottom) continue;

            final float left  = (line == startLine) ? layout.getPrimaryHorizontal(start) : layout.getLineLeft(line);
            final float right = (line == endLine)   ? layout.getPrimaryHorizontal(end)   : layout.getLineRight(line);

            if (x >= Math.min(left, right) && x <= Math.max(left, right)) return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        final Editable text = getText();
        final Layout layout = getLayout();
        if (text == null || layout == null) return super.onTouchEvent(e);

        final float x = e.getX() - getTotalPaddingLeft() + getScrollX();
        final float y = e.getY() - getTotalPaddingTop() + getScrollY();

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedTokenSpan = findTokenSpanUnderTouch(text, layout, x, y);
                if (pressedTokenSpan == null) return super.onTouchEvent(e);
                if (!isFocused()) {
                    suppressImeOnNextFocus();
                    requestFocusFromTouch();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (pressedTokenSpan == null) return super.onTouchEvent(e);
                if (!isTouchInsideSpan(text, layout, pressedTokenSpan, x, y)) pressedTokenSpan = null;
                return true;

            case MotionEvent.ACTION_UP:
                if (pressedTokenSpan == null) return super.onTouchEvent(e);
                if (isTouchInsideSpan(text, layout, pressedTokenSpan, x, y)) pressedTokenSpan.onClick();
                pressedTokenSpan = null;
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (pressedTokenSpan != null) { pressedTokenSpan = null; return true; }
                return super.onTouchEvent(e);

            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        //Never let users select text
        selEnd = selStart;

        Editable text = getText();
        if (text != null) {
            //Make sure if we are in a span, we select the spot 1 space after the span end
            TokenImageSpan[] spans = text.getSpans(selStart, selEnd, TokenImageSpan.class);
            for (TokenImageSpan span : spans) {
                int spanEnd = text.getSpanEnd(span);
                if (selStart <= spanEnd && text.getSpanStart(span) < selStart) {
                    if (spanEnd == text.length())
                        setSelection(spanEnd);
                    else
                        setSelection(spanEnd + 1);
                    return;
                }
            }

        }

        super.onSelectionChanged(selStart, selEnd);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        lastLayout = getLayout(); //Used for checking text positions
    }

    /**
     * Collapse the view by removing all the tokens not on the first line. Displays a "+x" token.
     * Restores the hidden tokens when the view gains focus.
     *
     * @param hasFocus boolean indicating whether we have the focus or not.
     */
    public void performCollapse(boolean hasFocus) {
        internalEditInProgress = true;
        if (!hasFocus  && getObjects().size() > 1) {
            // Display +x thingy/ellipse if appropriate
            final Editable text = getText();
            if (text != null && hiddenContent == null && lastLayout != null) {

                //Ellipsize copies spans, so we need to stop listening to span changes here
                text.removeSpan(spanWatcher);

                Spanned ellipsized = SpanUtils.ellipsizeWithSpans(countSpan, getObjects().size(),
                        lastLayout.getPaint(), text, maxTextWidth());

                if (ellipsized != null) {
                    hiddenContent = new SpannableStringBuilder(text);
                    setText(ellipsized);
                    TextUtils.copySpansFrom(ellipsized, 0, ellipsized.length(),
                            TokenImageSpan.class, getText(), 0);
                    TextUtils.copySpansFrom(text, 0, hiddenContent.length(),
                            TokenImageSpan.class, hiddenContent, 0);
                    hiddenContent.setSpan(spanWatcher, 0, hiddenContent.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else {
                    getText().setSpan(spanWatcher, 0, getText().length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        } else {
            if (hiddenContent != null) {
                setText(hiddenContent);
                TextUtils.copySpansFrom(hiddenContent, 0, hiddenContent.length(),
                        TokenImageSpan.class, getText(), 0);
                hiddenContent = null;

                // Avoid cursor jump (0 -> end) during expand
                final boolean wasCursorVisible = isCursorVisible();
                setCursorVisible(false);

                post(() -> {
                    Editable t = getText();
                    if (t != null) setSelection(t.length());
                    setCursorVisible(wasCursorVisible);
                });

                TokenSpanWatcher[] watchers = getText().getSpans(0, getText().length(), TokenSpanWatcher.class);
                if (watchers.length == 0) {
                    //Span watchers can get removed in setText
                    getText().setSpan(spanWatcher, 0, getText().length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        }
        internalEditInProgress = false;
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);

        // Collapse the view to a single line
        if (allowCollapse) performCollapse(hasFocus);
    }

    @SuppressWarnings("unchecked cast")
    @Override
    protected CharSequence convertSelectionToString(Object object) {
        selectedObject = (T) object;
        return "";
    }

    protected TokenImageSpan buildSpanForObject(T obj) {
        if (obj == null) {
            return null;
        }
        View tokenView = getViewForObject(obj);
        return new TokenImageSpan(tokenView, obj);
    }

    @Override
    protected void replaceText(CharSequence ignore) {
        clearComposingText();

        // Don't build a token for an empty String
        if (selectedObject == null || selectedObject.toString().equals("")) return;

        TokenImageSpan tokenSpan = buildSpanForObject(selectedObject);

        Editable editable = getText();
        Range candidateRange = getCurrentCandidateTokenRange();

        String original = TextUtils.substring(editable, candidateRange.start, candidateRange.end);

        //Keep track of  replacements for a bug workaround
        if (original.length() > 0) {
            lastCompletionText = original;
        }

        if (editable != null) {
            internalEditInProgress = true;
            if (tokenSpan == null) {
                editable.replace(candidateRange.start, candidateRange.end, "");
            } else if (shouldIgnoreToken(tokenSpan.getToken())) {
                editable.replace(candidateRange.start, candidateRange.end, "");
                if (listener != null) {
                    listener.onTokenIgnored(tokenSpan.getToken());
                }
            } else {
                SpannableStringBuilder ssb = new SpannableStringBuilder(tokenizer.wrapTokenValue(tokenToString(tokenSpan.token)));
                editable.replace(candidateRange.start, candidateRange.end, ssb);
                editable.setSpan(tokenSpan, candidateRange.start, candidateRange.start + ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                editable.insert(candidateRange.start + ssb.length(), " ");
            }
            internalEditInProgress = false;
        }
    }

    @Override
    public boolean extractText(@NonNull ExtractedTextRequest request, @NonNull ExtractedText outText) {
        try {
            return super.extractText(request, outText);
        } catch (IndexOutOfBoundsException ex) {
            Log.d(TAG, "extractText hit IndexOutOfBoundsException. This may be normal.", ex);
            return false;
        }
    }

    /**
     * Append a token object to the object list. May only be called from the main thread.
     *
     * @param object the object to add to the displayed tokens
     */
    @UiThread
    public void addObjectSync(T object) {
        if (object == null) return;
        if (shouldIgnoreToken(object)) {
            if (listener != null) {
                listener.onTokenIgnored(object);
            }
            return;
        }
        if (tokenLimit != -1 && getObjects().size() == tokenLimit) return;
        insertSpan(buildSpanForObject(object));
        if (getText() != null && isFocused()) setSelection(getText().length());
    }

    /**
     * Append a token object to the object list. Object will be added on the main thread.
     *
     * @param object the object to add to the displayed tokens
     */
    public void addObjectAsync(final T object) {
        post(new Runnable() {
            @Override
            public void run() {
                addObjectSync(object);
            }
        });
    }

    /**
     * Remove an object from the token list. Will remove duplicates if present or do nothing if no
     * object is present in the view. Uses {@link Object#equals(Object)} to find objects. May only
     * be called from the main thread
     *
     * @param object object to remove, may be null or not in the view
     */
    @UiThread
    public void removeObjectSync(T object) {
        //To make sure all the appropriate callbacks happen, we just want to piggyback on the
        //existing code that handles deleting spans when the text changes
        ArrayList<Editable>texts = new ArrayList<>();
        //If there is hidden content, it's important that we update it first
        if (hiddenContent != null) {
            texts.add(hiddenContent);
        }
        if (getText() != null) {
            texts.add(getText());
        }

        // If the object is currently visible, remove it
        for (Editable text: texts) {
            TokenImageSpan[] spans = text.getSpans(0, text.length(), TokenImageSpan.class);
            for (TokenImageSpan span : spans) {
                if (span.getToken().equals(object)) {
                    removeSpan(text, span);
                }
            }
        }

        updateCountSpan();
    }

    /**
     * Remove an object from the token list. Will remove duplicates if present or do nothing if no
     * object is present in the view. Uses {@link Object#equals(Object)} to find objects. Object
     * will be added on the main thread
     *
     * @param object object to remove, may be null or not in the view
     */
    public void removeObjectAsync(final T object) {
        post(new Runnable() {
            @Override
            public void run() {
                removeObjectSync(object);
            }
        });
    }

    /**
     * Remove all objects from the token list. Objects will be removed on the main thread.
     */
    public void clearAsync() {
        post(new Runnable() {
            @Override
            public void run() {
                for (T object: getObjects()) {
                    removeObjectSync(object);
                }
            }
        });
    }

    /**
     * Set the count span the current number of hidden objects
     */
    private void updateCountSpan() {
        Editable text = getText();

        int visibleCount = getText().getSpans(0, getText().length(), TokenImageSpan.class).length;
        countSpan.setCount(getObjects().size() - visibleCount);

        SpannableStringBuilder spannedCountText = new SpannableStringBuilder(countSpan.getCountText());
        spannedCountText.setSpan(countSpan, 0, spannedCountText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        internalEditInProgress = true;
        int countStart = text.getSpanStart(countSpan);
        if (countStart != -1) {
            //Span is in the text, replace existing text
            //This will also remove the span if the count is 0
            text.replace(countStart, text.getSpanEnd(countSpan), spannedCountText);
        } else {
            text.append(spannedCountText);
        }

        internalEditInProgress = false;
    }

    /**
     * Remove a span from the current EditText and fire the appropriate callback
     *
     * @param text Editable to remove the span from
     * @param span TokenImageSpan to be removed
     */
    private void removeSpan(Editable text, TokenImageSpan span) {
        //We usually add whitespace after a token, so let's try to remove it as well if it's present
        int end = text.getSpanEnd(span);
        if (end < text.length() && text.charAt(end) == ' ') {
            end += 1;
        }

        internalEditInProgress = true;
        text.delete(text.getSpanStart(span), end);
        internalEditInProgress = false;

        if (allowCollapse && !isFocused()) {
            updateCountSpan();
        }
    }

    /**
     * Insert a new span for an Object
     *
     * @param tokenSpan span to insert
     */
    private void insertSpan(TokenImageSpan tokenSpan) {
        CharSequence ssb = tokenizer.wrapTokenValue(tokenToString(tokenSpan.token));

        Editable editable = getText();
        if (editable == null) return;

        // If we haven't hidden any objects yet, we can try adding it
        if (hiddenContent == null) {
            internalEditInProgress = true;
            int offset = editable.length();

            Range currentRange = getCurrentCandidateTokenRange();
            if (currentRange.length() > 0) {
                // The user has entered some text that has not yet been tokenized.
                // Find the beginning of this text and insert the new token there.
                offset = currentRange.start;
            }

            editable.insert(offset, ssb);
            editable.insert(offset  + ssb.length(), " ");
            editable.setSpan(tokenSpan, offset, offset + ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            internalEditInProgress = false;
        } else {
            CharSequence tokenText = tokenizer.wrapTokenValue(tokenToString(tokenSpan.getToken()));
            int start = hiddenContent.length();
            hiddenContent.append(tokenText);
            hiddenContent.append(" ");
            hiddenContent.setSpan(tokenSpan, start, start + tokenText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateCountSpan();
        }
    }

    protected class TokenImageSpan extends ViewSpan implements NoCopySpan {
        private T token;

        @SuppressWarnings("WeakerAccess")
        public TokenImageSpan(View d, T token) {
            super(d, TokenCompleteTextView.this);
            this.token = token;
        }

        @SuppressWarnings("WeakerAccess")
        public T getToken() {
            return this.token;
        }

        @SuppressWarnings("WeakerAccess")
        public void onClick() {
            Editable text = getText();
            if (text == null) return;

            if (getSelectionStart() != text.getSpanEnd(this)) {
                //Make sure the selection is not in the middle of the span
                setSelection(text.getSpanEnd(this));
            }
        }
    }

    public interface TokenListener<T> {
        void onTokenAdded(T token);
        void onTokenRemoved(T token);
        void onTokenIgnored(T token);
    }

    private class TokenSpanWatcher implements SpanWatcher {

        @SuppressWarnings("unchecked cast")
        @Override
        public void onSpanAdded(Spannable text, Object what, int start, int end) {
            if (what instanceof TokenCompleteTextView<?>.TokenImageSpan && !savingState) {
                TokenImageSpan token = (TokenImageSpan) what;

                // If we're not focused: collapse the view if necessary
                if (!isFocused() && allowCollapse) performCollapse(false);

                if (listener != null)
                    listener.onTokenAdded(token.getToken());
            }
        }

        @SuppressWarnings("unchecked cast")
        @Override
        public void onSpanRemoved(Spannable text, Object what, int start, int end) {
            if (what instanceof TokenCompleteTextView<?>.TokenImageSpan && !savingState) {
                TokenImageSpan token = (TokenImageSpan) what;

                if (listener != null)
                    listener.onTokenRemoved(token.getToken());
            }
        }

        @Override
        public void onSpanChanged(Spannable text, Object what,
                                  int oldStart, int oldEnd, int newStart, int newEnd) {
        }
    }

    private class TokenTextWatcher implements TextWatcher {
        ArrayList<TokenImageSpan> spansToRemove = new ArrayList<>();

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // count > 0 means something will be deleted
            if (count > 0 && getText() != null) {
                Editable text = getText();

                int end = start + count;

                TokenImageSpan[] spans = text.getSpans(start, end, TokenImageSpan.class);

                //NOTE: I'm not completely sure this won't cause problems if we get stuck in a text changed loop
                //but it appears to work fine. Spans will stop getting removed if this breaks.
                ArrayList<TokenImageSpan> spansToRemove = new ArrayList<>();
                for (TokenImageSpan token : spans) {
                    if (text.getSpanStart(token) < end && start < text.getSpanEnd(token)) {
                        spansToRemove.add(token);
                    }
                }
                this.spansToRemove = spansToRemove;
            }
        }

        @Override
        public void afterTextChanged(Editable text) {
            ArrayList<TokenImageSpan> spansCopy = new ArrayList<>(spansToRemove);
            spansToRemove.clear();
            for (TokenImageSpan token : spansCopy) {
                //Only remove it if it's still present
                if (text.getSpanStart(token) != -1 && text.getSpanEnd(token) != -1) {
                    removeSpan(text, token);
                }

            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    protected List<Serializable> getSerializableObjects() {
        List<Serializable> serializables = new ArrayList<>();
        for (Object obj : getObjects()) {
            if (obj instanceof Serializable) {
                serializables.add((Serializable) obj);
            } else {
                Log.e(TAG, "Unable to save '" + obj + "'");
            }
        }
        if (serializables.size() != getObjects().size()) {
            String message = "You should make your objects Serializable or Parcelable or\n" +
                    "override getSerializableObjects and convertSerializableArrayToObjectArray";
            Log.e(TAG, message);
        }

        return serializables;
    }

    @SuppressWarnings("unchecked")
    protected List<T> convertSerializableObjectsToTypedObjects(List s) {
        return (List<T>) s;
    }

    //Used to determine if we can use the Parcelable interface
    private Class reifyParameterizedTypeClass() {
        //Borrowed from http://codyaray.com/2013/01/finding-generic-type-parameters-with-guava

        //Figure out what class of objects we have
        Class<?> viewClass = getClass();
        while (!viewClass.getSuperclass().equals(TokenCompleteTextView.class)) {
            viewClass = viewClass.getSuperclass();
        }

        // This operation is safe. Because viewClass is a direct sub-class, getGenericSuperclass() will
        // always return the Type of this class. Because this class is parameterized, the cast is safe
        ParameterizedType superclass = (ParameterizedType) viewClass.getGenericSuperclass();
        Type type = superclass.getActualTypeArguments()[0];
        return (Class)type;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        //We don't want to save the listeners as part of the parent
        //onSaveInstanceState, so remove them first
        removeListeners();

        //Apparently, saving the parent state on 2.3 mutates the spannable
        //prevent this mutation from triggering add or removes of token objects ~mgod
        savingState = true;
        Parcelable superState = super.onSaveInstanceState();
        savingState = false;
        SavedState state = new SavedState(superState);

        state.allowCollapse = allowCollapse;
        state.performBestGuess = performBestGuess;
        state.tokenizer = tokenizer;

        // Save full text + ordered token span ranges so restore can reattach token spans without changing the text.
        CharSequence content = getContentText();
        state.contentText = content == null ? "" : content.toString();

        Spanned spanned = (hiddenContent != null) ? hiddenContent : getText();
        @SuppressWarnings("unchecked")
        TokenImageSpan[] spans = (spanned == null)
            ? (TokenImageSpan[]) new TokenCompleteTextView.TokenImageSpan[0]
            : spanned.getSpans(0, spanned.length(), TokenImageSpan.class);

        if (spanned != null) {
            Arrays.sort(spans, (a, b) -> Integer.compare(spanned.getSpanStart(a), spanned.getSpanStart(b)));
        }

        state.tokenStarts = new int[spans.length];
        state.tokenEnds = new int[spans.length];

        ArrayList<Object> orderedObjects = new ArrayList<>(spans.length);
        for (int i = 0; i < spans.length; i++) {
            TokenImageSpan s = spans[i];
            state.tokenStarts[i] = spanned.getSpanStart(s);
            state.tokenEnds[i] = spanned.getSpanEnd(s);
            orderedObjects.add(s.getToken());
        }

        Class parameterizedClass = reifyParameterizedTypeClass();
        //Our core array is Parcelable, so use that interface
        if (Parcelable.class.isAssignableFrom(parameterizedClass)) {
            state.parcelableClassName = parameterizedClass.getName();
            state.baseObjects = orderedObjects;
        } else {
            //Fallback on Serializable
            state.parcelableClassName = SavedState.SERIALIZABLE_PLACEHOLDER;

            ArrayList<Serializable> serializables = new ArrayList<>(orderedObjects.size());
            int[] starts = new int[orderedObjects.size()];
            int[] ends = new int[orderedObjects.size()];
            int n = 0;

            for (int i = 0; i < orderedObjects.size(); i++) {
                Object obj = orderedObjects.get(i);
                if (obj instanceof Serializable) {
                    serializables.add((Serializable) obj);
                    starts[n] = state.tokenStarts[i];
                    ends[n] = state.tokenEnds[i];
                    n++;
                }
            }

            state.baseObjects = serializables;
            // Keep tokenStarts/tokenEnds aligned with filtered baseObjects
            state.tokenStarts = Arrays.copyOf(starts, n);
            state.tokenEnds = Arrays.copyOf(ends, n);

            if (n != orderedObjects.size()) {
                Log.e(TAG, "Some tokens not Serializable/Parcelable; state restore may be incorrect");
            }
        }

        //So, when the screen is locked or some other system event pauses execution,
        //onSaveInstanceState gets called, but it won't restore state later because the
        //activity is still in memory, so make sure we add the listeners again
        //They should not be restored in onInstanceState if the app is actually killed
        //as we removed them before the parent saved instance state, so our adding them in
        //onRestoreInstanceState is good.
        addListeners();

        return state;
    }

    private void restoreTokenSpansFromState(
        @NonNull Editable editable,
        @NonNull List<T> objects,
        @Nullable int[] starts,
        @Nullable int[] ends
    ) {
        // Defensive: clear any existing token spans before restoring saved ranges.
        TokenImageSpan[] existing = editable.getSpans(0, editable.length(), TokenImageSpan.class);
        for (TokenImageSpan s : existing) {
            editable.removeSpan(s);
        }

        if (objects.isEmpty()) return;

        int count = objects.size();
        if (starts == null || ends == null || starts.length != count || ends.length != count) {
            return;
        }

        for (int i = 0; i < count; i++) {
            int start = starts[i];
            int end = ends[i];

            if (start < 0 || end > editable.length() || start >= end) continue;

            TokenImageSpan span = buildSpanForObject(objects.get(i));
            if (span != null) {
                editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        allowCollapse = ss.allowCollapse;
        performBestGuess = ss.performBestGuess;
        tokenizer = ss.tokenizer;

        // Ensure we don't register duplicate watchers during restore.
        removeListeners();

        List<T> objects;
        if (SavedState.SERIALIZABLE_PLACEHOLDER.equals(ss.parcelableClassName)) {
            objects = convertSerializableObjectsToTypedObjects(ss.baseObjects);
        } else {
            objects = (List<T>)ss.baseObjects;
        }

        savingState = true;
        internalEditInProgress = true;
        try {
            // Start from a clean expanded state. Collapse (and hiddenContent) will be rebuilt after restore if needed.
            hiddenContent = null;

            // Restore text once, then re-attach token spans by saved ranges.
            setText(ss.contentText != null ? ss.contentText : "");

            Editable editable = getText();
            if (editable != null && objects != null) {
                restoreTokenSpansFromState(editable, objects, ss.tokenStarts, ss.tokenEnds);
                setSelection(editable.length());
            }
        } finally {
            internalEditInProgress = false;
            savingState = false;
            addListeners();
        }

        // Collapse the view if necessary
        if (!isFocused() && allowCollapse) {
            post(new Runnable() {
                @Override
                public void run() {
                    //Resize the view and display the +x if appropriate
                    performCollapse(isFocused());
                }
            });
        }
    }

    /**
     * Handle saving the token state
     */
    private static class SavedState extends BaseSavedState {
        static final String SERIALIZABLE_PLACEHOLDER = "Serializable";

        boolean allowCollapse;
        boolean performBestGuess;
        String parcelableClassName;
        List<?> baseObjects;
        String tokenizerClassName;
        Tokenizer tokenizer;
        @Nullable String contentText;
        @Nullable int[] tokenStarts;
        @Nullable int[] tokenEnds;

        @SuppressWarnings("unchecked")
        SavedState(Parcel in) {
            super(in);
            allowCollapse = in.readInt() != 0;
            performBestGuess = in.readInt() != 0;
            parcelableClassName = in.readString();
            if (SERIALIZABLE_PLACEHOLDER.equals(parcelableClassName)) {
                baseObjects = (ArrayList)in.readSerializable();
            } else {
                try {
                    ClassLoader loader = Class.forName(parcelableClassName).getClassLoader();
                    baseObjects = in.readArrayList(loader);
                } catch (ClassNotFoundException ex) {
                    //This should really never happen, class had to be available to get here
                    throw new RuntimeException(ex);
                }
            }
            tokenizerClassName = in.readString();
            try {
                ClassLoader loader = Class.forName(tokenizerClassName).getClassLoader();
                tokenizer = in.readParcelable(loader);
            } catch (ClassNotFoundException ex) {
                //This should really never happen, class had to be available to get here
                throw new RuntimeException(ex);
            }
            // Backward-compatible: older parcels don't contain the fields below.
            if (in.dataAvail() > 0) {
                contentText = in.readString();
                tokenStarts = in.createIntArray();
                tokenEnds = in.createIntArray();
            }
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(allowCollapse ? 1 : 0);
            out.writeInt(performBestGuess ? 1 : 0);
            if (SERIALIZABLE_PLACEHOLDER.equals(parcelableClassName)) {
                out.writeString(SERIALIZABLE_PLACEHOLDER);
                out.writeSerializable((Serializable)baseObjects);
            } else {
                out.writeString(parcelableClassName);
                out.writeList(baseObjects);
            }
            out.writeString(tokenizer.getClass().getCanonicalName());
            out.writeParcelable(tokenizer, 0);
            out.writeString(contentText);
            out.writeIntArray(tokenStarts);
            out.writeIntArray(tokenEnds);
        }

        @Override
        public String toString() {
            String str = "TokenCompleteTextView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " tokens=" + baseObjects;
            return str + "}";
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Checks if selection can be deleted. This method is called from TokenInputConnection .
     * @param beforeLength the number of characters before the current selection end to check
     * @return true if there are no non-deletable pieces of the section
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canDeleteSelection(int beforeLength) {
        if (getObjects().size() < 1) return true;

        // if beforeLength is 1, we either have no selection or the call is coming from OnKey Event.
        // In these scenarios, getSelectionStart() will return the correct value.

        int endSelection = getSelectionEnd();
        int startSelection = beforeLength == 1 ? getSelectionStart() : endSelection - beforeLength;

        Editable text = getText();
        TokenImageSpan[] spans = text.getSpans(0, text.length(), TokenImageSpan.class);

        // Iterate over all tokens and allow the deletion
        // if there are no tokens not removable in the selection
        for (TokenImageSpan span : spans) {
            int startTokenSelection = text.getSpanStart(span);
            int endTokenSelection = text.getSpanEnd(span);

            // moving on, no need to check this token
            if (isTokenRemovable(span.token)) continue;

            if (startSelection == endSelection) {
                // Delete single
                if (endTokenSelection + 1 == endSelection) {
                    return false;
                }
            } else {
                // Delete range
                // Don't delete if a non removable token is in range
                if (startSelection <= startTokenSelection
                        && endTokenSelection + 1 <= endSelection) {
                    return false;
                }
            }
        }
        return true;
    }

    private class TokenInputConnection extends InputConnectionWrapper {

        TokenInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        // This will fire if the soft keyboard delete key is pressed.
        // The onKeyPressed method does not always do this.
        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // Shouldn't be able to delete any text with tokens that are not removable
            if (!canDeleteSelection(beforeLength)) return false;

            return super.deleteSurroundingText(beforeLength, afterLength);
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            //There's an issue with some keyboards where they will try to insert the first word
            //of the prefix as the composing text
            CharSequence hint = getHint();
            if (hint != null && text != null) {
                String firstWord = hint.toString().trim().split(" ")[0];
                if (firstWord.length() > 0 && firstWord.equals(text.toString())) {
                    text = ""; //It was trying to use th hint, so clear that text
                }
            }

            //Also, some keyboards don't correctly respect the replacement if the replacement
            //is the same number of characters as the replacement span
            //We need to ignore this value if it's available
            if (lastCompletionText != null && text != null &&
                    text.length() == lastCompletionText.length() + 1 &&
                    text.toString().startsWith(lastCompletionText)) {
                text = text.subSequence(text.length() - 1, text.length());
                lastCompletionText = null;
            }

            return super.setComposingText(text, newCursorPosition);
        }
    }
}
