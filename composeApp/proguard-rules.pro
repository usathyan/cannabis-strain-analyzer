# BudMash ProGuard Rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used with serialization
-keep,includedescriptorclasses class com.budmash.**$$serializer { *; }
-keepclassmembers class com.budmash.** {
    *** Companion;
}
-keepclasseswithmembers class com.budmash.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# Missing classes for Ktor debug detection (not needed on Android)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep data models
-keep class com.budmash.data.** { *; }
-keep class com.budmash.llm.** { *; }
