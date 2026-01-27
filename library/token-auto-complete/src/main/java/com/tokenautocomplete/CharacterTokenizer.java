package com.tokenautocomplete;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer with configurable array of characters to tokenize on.
 *
 * Created on 2/3/15.
 * @author mgod
 */
public class CharacterTokenizer implements Tokenizer {
    private ArrayList<Character> splitChar;
    private String tokenTerminator;

    @SuppressWarnings("WeakerAccess")
    public CharacterTokenizer(List<Character> splitChar, String tokenTerminator){
        super();
        this.splitChar = new ArrayList<>(splitChar);
        this.tokenTerminator = tokenTerminator;
    }

    @Override
    public boolean containsTokenTerminator(CharSequence charSequence) {
        for (int i = 0; i < charSequence.length(); ++i) {
            if (splitChar.contains(charSequence.charAt(i))) {
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

        int tokenStart = start;

        for (int cursor = start; cursor < end; ++cursor) {
            char character = charSequence.charAt(cursor);

            //Avoid including leading whitespace, tokenStart will match the cursor as long as we're at the start
            if (tokenStart == cursor && Character.isWhitespace(character)) {
                tokenStart = cursor + 1;
            }

            //Either this is a split character, or we contain some content and are at the end of input
            if (splitChar.contains(character) || cursor == end - 1) {
                boolean hasTokenContent =
                        //There is token content befor the current character
                        cursor > tokenStart ||
                        //If the current single character is valid token content, not a split char or whitespace
                        (cursor == tokenStart && !splitChar.contains(character));
                if (hasTokenContent) {
                    //There is some token content
                    //Add one to range end as the end of the ranges is not inclusive
                    result.add(new Range(tokenStart, cursor + 1));
                }

                tokenStart = cursor + 1;
            }
        }

        return result;
    }

    @Override
    @NonNull
    public CharSequence wrapTokenValue(CharSequence text) {
        CharSequence wrappedText = text + tokenTerminator;

        if (text instanceof Spanned) {
            SpannableString sp = new SpannableString(wrappedText);
            TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                    Object.class, sp, 0);
            return sp;
        } else {
            return wrappedText;
        }
    }

    public static final Parcelable.Creator<CharacterTokenizer> CREATOR = new Parcelable.Creator<CharacterTokenizer>() {
        @SuppressWarnings("unchecked")
        public CharacterTokenizer createFromParcel(Parcel in) {
            return new CharacterTokenizer(in);
        }

        public CharacterTokenizer[] newArray(int size) {
            return new CharacterTokenizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings({"WeakerAccess", "unchecked"})
    CharacterTokenizer(Parcel in) {
        this(in.readArrayList(Character.class.getClassLoader()), in.readString());
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeList(splitChar);
        parcel.writeString(tokenTerminator);
    }
}
