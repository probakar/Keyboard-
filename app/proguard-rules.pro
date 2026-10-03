# ============================================================================
#  CustomBoard ProGuard / R8 rules
# ============================================================================

-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# ---------------------------------------------------------------------------
# Keep the InputMethodService and everything the Android framework instantiates
# by name from AndroidManifest.xml / XML resources.
# ---------------------------------------------------------------------------
-keep public class * extends android.inputmethodservice.InputMethodService
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.preference.PreferenceFragmentCompat
-keep public class * extends androidx.preference.Preference

# Custom views are inflated from XML by reflection.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    *** get*();
}

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Kotlin / Coroutines
# ---------------------------------------------------------------------------
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---------------------------------------------------------------------------
# OkHttp
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---------------------------------------------------------------------------
# Google ML Kit (on-device AI). Each SDK ships consumer rules for required reflection;
# avoid blanket keep rules so unused transitive classes can still be removed by R8.
# ---------------------------------------------------------------------------
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ---------------------------------------------------------------------------
# Keep our data/model classes (serialized to JSON for backup & AI payloads)
# ---------------------------------------------------------------------------
-keep class com.customboard.keyboard.clipboard.ClipboardEntity { *; }
-keep class com.customboard.keyboard.keyboard.model.** { *; }
-keep class com.customboard.keyboard.ai.model.** { *; }
-keep class com.customboard.keyboard.theme.ThemeColors { *; }

# Enum values are used by name in preferences.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep annotations & signatures needed at runtime
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions

# Strip verbose logging from release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
