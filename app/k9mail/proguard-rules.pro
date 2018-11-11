# Add project specific ProGuard rules here.

-dontobfuscate

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Library specific rules
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**
-dontnote com.squareup.moshi.**
-dontnote com.github.amlcurran.showcaseview.**
-dontnote de.cketti.safecontentresolver.**
-dontnote com.tokenautocomplete.**

-dontwarn okio.**
-dontwarn com.squareup.moshi.**

# Project specific rules
-dontnote com.fsck.k9.PRNGFixes
-dontnote com.fsck.k9.ui.messageview.**
-dontnote com.fsck.k9.view.**

# from https://github.com/google/guava/wiki/UsingProGuardWithGuava
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}
# used only by errorprone annotations
-dontwarn javax.lang.model.element.Modifier
# not sure why this is needed for guava
-dontwarn java.lang.ClassValue

-keep public class org.openintents.openpgp.**

-keepclassmembers class * extends android.support.v7.widget.SearchView {
   public <init>(android.content.Context);
}
