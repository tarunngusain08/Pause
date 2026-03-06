# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
# Keep Room entities and DAOs
-keep class com.pause.app.data.db.entity.** { *; }
-keep class com.pause.app.data.db.dao.** { *; }
