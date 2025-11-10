# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ================================
# Room Database
# ================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# 保留数据库实体类
-keep class com.nyapass.loader.data.model.** { *; }
-keep class com.nyapass.loader.data.database.** { *; }

# ================================
# OkHttp & Okio
# ================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ================================
# Kotlin Coroutines
# ================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ================================
# Jetpack Compose
# ================================
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.material3.**

# ================================
# Gson
# ================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================================
# 保留应用核心类
# ================================
-keep class com.nyapass.loader.** { *; }
-keep class com.nyapass.loader.service.** { *; }
-keep class com.nyapass.loader.download.** { *; }

# ================================
# 保留序列化相关
# ================================
-keepattributes *Annotation*, InnerClasses
-keepattributes SourceFile, LineNumberTable
-keepattributes Signature, Exceptions, *Annotation*

# ================================
# 优化相关
# ================================
# 保留调试信息（可选，用于崩溃报告）
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
