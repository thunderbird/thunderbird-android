package com.tokenautocomplete;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class TagTokenizer implements Tokenizer {

    private ArrayList<Character> tagPrefixes;

    TagTokenizer() {
        this(Arrays.asList('@', '#'));
    }

    public TagTokenizer(List<Character> tagPrefixes){
        super();
        this.tagPrefixes = new ArrayList<>(tagPrefixes);
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean isTokenTerminator(char character) {
        //Allow letters, numbers and underscores
        return !Character.isLetterOrDigit(character) && character != '_';
    }

    @Override
    public boolean containsTokenTerminator(CharSequence charSequence) {
        for (int i = 0; i < charSequence.length(); ++i) {
            if (isTokenTerminator(charSequence.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    @Override
    @NonNull
    public List<Range> findTokenRanges(CharSequence charSequence, int start, int end) {
        ArrayList<Range>result = new ArrayList<>();

        if (start == end) {
            //Can't have a 0 length token
            return result;
        }

        int tokenStart = Integer.MAX_VALUE;

        for (int cursor = start; cursor < end; ++cursor) {
            char character = charSequence.charAt(cursor);

            //Either this is a terminator, or we contain some content and are at the end of input
            if (isTokenTerminator(character)) {
                //Is there some token content? Might just be two terminators in a row
                if (cursor - 1 > tokenStart) {
                    result.add(new Range(tokenStart, cursor));
                }

                //mark that we don't have a candidate token start any more
                tokenStart = Integer.MAX_VALUE;
            }

            //Set tokenStart when we hit a tag prefix
            if (tagPrefixes.contains(character)) {
                tokenStart = cursor;
            }
        }

        if (end > tokenStart) {
            //There was unterminated text after a start of token
            result.add(new Range(tokenStart, end));
        }

        return result;
    }

    @Override
    @NonNull
    public CharSequence wrapTokenValue(CharSequence text) {
        return text;
    }

    public static final Parcelable.Creator<TagTokenizer> CREATOR = new Parcelable.Creator<TagTokenizer>() {
        @SuppressWarnings("unchecked")
        public TagTokenizer createFromParcel(Parcel in) {
            return new TagTokenizer(in);
        }

        public TagTokenizer[] newArray(int size) {
            return new TagTokenizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings({"WeakerAccess", "unchecked"})
    TagTokenizer(Parcel in) {
        this(in.readArrayList(Character.class.getClassLoader()));
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeList(tagPrefixes);
    }
}
