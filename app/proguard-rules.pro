# Reglas de kotlinx.serialization (por si se activa minify en release)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.rutaalacima.app.data.content.** { *** Companion; }
-keepclasseswithmembers class com.rutaalacima.app.data.content.** { kotlinx.serialization.KSerializer serializer(...); }
