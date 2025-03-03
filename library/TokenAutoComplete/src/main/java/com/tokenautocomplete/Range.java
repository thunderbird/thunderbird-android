package com.tokenautocomplete;

import java.util.Locale;

class Range {
    public final int start;
    public final int end;

    Range(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH,
                    "Start (%d) cannot be greater than end (%d)", start, end));
        }
        this.start = start;
        this.end = end;
    }

    public int length() {
        return end - start;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || !(obj instanceof Range)) {
            return false;
        }

        Range other = (Range) obj;
        return other.start == start && other.end == end;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "[%d..%d]", start, end);
    }
}
