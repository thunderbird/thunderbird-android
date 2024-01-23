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

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Project specific rules
-dontnote com.fsck.k9.ui.messageview.**
-dontnote com.fsck.k9.view.**

-assumevalues class * extends android.view.View {
    boolean isInEditMode() return false;
}

-keep public class org.openintents.openpgp.**

-keepclassmembers class * extends androidx.appcompat.widget.SearchView {
    public <init>(android.content.Context);
}

-keep class com.fsck.k9.mail.oauth.XOAuth2Response { *; }

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

-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable
-dontwarn org.apache.http.client.methods.CloseableHttpResponse
-dontwarn org.slf4j.impl.StaticLoggerBinder

-keep,allowshrinking class com.tokenautocomplete.TokenCompleteTextView
