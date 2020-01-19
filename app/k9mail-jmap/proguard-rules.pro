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
-keep class rs.ltt.jmap.common.** {*;}

-dontwarn okio.**
-dontwarn com.squareup.moshi.**

# Project specific rules
-dontnote com.fsck.k9.ui.messageview.**
-dontnote com.fsck.k9.view.**

-keep public class org.openintents.openpgp.**

-keepclassmembers class * extends androidx.appcompat.widget.SearchView {
    public <init>(android.content.Context);
}

# okhttp rules
# see: https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform
