# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room Database kuralları
-keep class com.alperyuceer.komik_replikler.Replik { *; }
-keep class com.alperyuceer.komik_replikler.ReplikDao { *; }
-keep class com.alperyuceer.komik_replikler.ReplikDatabase { *; }

# Kotlin serialization için gerekli kurallar
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# AdMob için gerekli kurallar
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep public class com.google.ads.** {
    public *;
}

# MediaPlayer için gerekli kurallar
-keep class * extends android.media.MediaPlayer { *; }

# Genel Android kuralları
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Enum'lar için gerekli kurallar
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Uygulama özel sınıfları koru
-keep class com.alperyuceer.komik_replikler.AudioEncryption { *; }
-keep class com.alperyuceer.komik_replikler.KategoriConverter { *; }

# Hata ayıklama için kaynak dosya adlarını ve satır numaralarını koru
-keepattributes SourceFile,LineNumberTable

# Crash raporlama için stack trace'leri koru
-renamesourcefileattribute SourceFile

# LoudnessCodecController için ek kurallar
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener

# AdMob için ek kurallar
-keepclassmembers class com.google.android.gms.internal.ads.** {
    *;
}
-keep class com.google.android.gms.internal.ads.** { *; }

# Tüm uyarıları yoksay
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.gms.ads.**

# MediaPlayer için ek kurallar
-keep class android.media.** { *; }
-dontwarn android.media.**