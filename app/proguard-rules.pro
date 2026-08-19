# ============================================================================
# ProGuard & R8 Optimization & Obfuscation Rules
# Application Package: com.example.findmyphonebyclaplauncher
# ============================================================================

# ----------------------------------------------------------------------------
# 1. General Optimization & Preservation Settings
# ----------------------------------------------------------------------------
# Preserve line numbers and source file names for accurate Crashlytics/stack trace deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve runtime annotations, generic type signatures, and inner classes (crucial for Retrofit/Gson/Reflection)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep @Keep annotation target classes and members
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ----------------------------------------------------------------------------
# 2. Android Core Components & Framework
# ----------------------------------------------------------------------------
# Standard Android components (Activities, Fragments, Services, Receivers, Application)
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ViewModels and Lifecycle
-keep public class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Custom Views under app package
-keep class com.example.findmyphonebyclaplauncher.ui.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ----------------------------------------------------------------------------
# 3. ViewBinding & DataBinding
# ----------------------------------------------------------------------------
# Retain generated ViewBinding classes and layout view references
-keep class com.example.findmyphonebyclaplauncher.databinding.** { *; }
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static ** bind(android.view.View);
    public static ** inflate(...);
    public ** getRoot();
}

# DataBinding rules
-keep class * extends androidx.databinding.ViewDataBinding { *; }
-keepclassmembers class **.BR {
    public static final int *;
}

# ----------------------------------------------------------------------------
# 4. Navigation Component
# ----------------------------------------------------------------------------
# Retain generated SafeArgs navigation directions and arguments
-keep class * extends androidx.navigation.NavArgs { *; }
-keep class com.example.findmyphonebyclaplauncher.**.*Directions { *; }
-keep class com.example.findmyphonebyclaplauncher.**.*Args { *; }

# ----------------------------------------------------------------------------
# 5. Data Models, POJOs & JSON Serialization (Gson / KotlinX Serialization)
# ----------------------------------------------------------------------------
# Keep all data models & domain POJOs intact (prevents field renaming/pruning)
-keep class com.example.findmyphonebyclaplauncher.data.model.** { *; }
-keep class com.example.findmyphonebyclaplauncher.data.** { *; }
-keep class com.example.findmyphonebyclaplauncher.domain.model.** { *; }
-keep class com.example.findmyphonebyclaplauncher.domain.** { *; }

# Gson rules
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# KotlinX Serialization rules (if used)
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    public static final kotlinx.serialization.KSerializer companion;
}
-keep class kotlinx.serialization.OuterSerializer { *; }
-dontwarn kotlinx.serialization.**

# ----------------------------------------------------------------------------
# 6. Local Storage (Room Database & DataStore / SharedPreferences)
# ----------------------------------------------------------------------------
# Room Database Entities & DAOs
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# DataStore & Preferences
-keep class * extends androidx.datastore.core.Serializer { *; }
-dontwarn androidx.datastore.**

# ----------------------------------------------------------------------------
# 7. Networking (Retrofit & OkHttp)
# ----------------------------------------------------------------------------
# Retrofit interface preservation
-keep interface com.example.findmyphonebyclaplauncher.data.api.** { *; }
-keepclassmembernames class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# OkHttp rules
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# ----------------------------------------------------------------------------
# 8. Asynchronous Processing (Kotlin Coroutines & RxJava)
# ----------------------------------------------------------------------------
# Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# RxJava rules (RxJava 2 / 3)
-keepclassmembers class rx.internal.util.unsafe.**ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class io.reactivex.rxjava3.internal.util.OpenHashSet {
    int keys;
}
-dontwarn io.reactivex.**
-dontwarn rx.**

# ----------------------------------------------------------------------------
# 9. Native Code (JNI), Enums, Parcelable & Serializable
# ----------------------------------------------------------------------------
# Native / JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Enum values (required for Enum.valueOf reflection)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable implementations
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ----------------------------------------------------------------------------
# 10. Third-Party Libraries (Glide, Lottie, Firebase, AdMob, BlurView, SDP/SSP, Shimmer)
# ----------------------------------------------------------------------------
# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keepclassmembers class * implements com.bumptech.glide.module.GlideModule {
    public void applyOptions(android.content.Context, com.bumptech.glide.GlideBuilder);
    public void registerComponents(android.content.Context, com.bumptech.glide.Glide, com.bumptech.glide.Registry);
}
-dontwarn com.bumptech.glide.**

# Lottie Animation
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Google Play Services & AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**

# Firebase (Analytics, Config, Crashlytics)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# BlurView
-keep class com.eightbitlab.blurview.** { *; }
-dontwarn com.eightbitlab.blurview.**

# Facebook Shimmer
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

# SDP & SSP Unit Libraries
-dontwarn com.intuit.sdp.**
-dontwarn com.intuit.ssp.**

# ----------------------------------------------------------------------------
# 11. Warning Suppression (-dontwarn)
# ----------------------------------------------------------------------------
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**