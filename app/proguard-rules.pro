# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Room Database
-keep class androidx.room.** { *; }
-keepclasseswithmembernames class * {
    @androidx.room.* <methods>;
}

# Kotlin
-keep class kotlin.Metadata { *; }

// Author: E Thrinadh Chowdary
