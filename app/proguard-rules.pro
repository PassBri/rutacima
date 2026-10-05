# ---------- kotlinx.serialization: modelos de contenido, sincronización y entidades ----------
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.rutaalacima.app.**$$serializer { *; }
-keepclassmembers class com.rutaalacima.app.** { *** Companion; }
-keepclasseswithmembers class com.rutaalacima.app.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers @kotlinx.serialization.Serializable class com.rutaalacima.app.** { *; }

# ---------- Room (entidades y DAO generados) ----------
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# ---------- WorkManager: los Workers se crean por reflexión ----------
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
