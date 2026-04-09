# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to the flags specified
# in /usr/local/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# config files in build.gradle.

# Keep Firebase model classes
-keepclassmembers class com.google.firebase.** { *; }
