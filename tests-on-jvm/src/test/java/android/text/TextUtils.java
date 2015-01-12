package android.text;

public class TextUtils {
    public static boolean isEmpty(CharSequence str) {
        return (str == null || str.length() == 0);
    }
}
