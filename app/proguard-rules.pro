# Moshi models use generated adapters; keep model metadata and adapters for R8.
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }

# Retrofit service methods are discovered by annotations/reflection.
-keep,allowobfuscation interface com.example.data.api.F1ApiService

-keepattributes Exceptions,InnerClasses,Signature
