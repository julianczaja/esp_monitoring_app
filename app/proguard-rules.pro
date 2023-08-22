-keepattributes Signature
-keep class sun.misc.Unsafe { *; }

### ------------------------ kotlinx.serialization ------------------------
# https://github.com/Kotlin/kotlinx.serialization/issues/1129
# Kotlin serialization looks up the generated serializer classes through a function on companion
# objects. The companions are looked up reflectively so we need to explicitly keep these functions.
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# If a companion has the serializer function, keep the companion field on the original type so that
# the reflective lookup succeeds.
-if class **.*$Companion {
  kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>.<2> {
  <1>.<2>$Companion Companion;
}

-keep class com.julianczaja.esp_monitoring_app.domain.model.** { *; }
-keep class kotlin.Result

-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# TODO: Waiting for new retrofit release to remove these rules (https://github.com/square/retrofit/issues/3751)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-keep class retrofit2.** { *; }
# https://github.com/google/gson/commit/43396e45fd1f03e408e0e83b168a72a0f3e0b84e#diff-5da161239475717e284b3a9a85e2f39256d739fb7564ae7fda7f79cee000c413
-keepclasseswithmembers,allowobfuscation,includedescriptorclasses class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
### -----------------------------------------------------------------------

# For debugging prod
#-dontobfuscate
#-dontoptimize
#-keepattributes SourceFile,LineNumberTable
