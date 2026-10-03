# Moshi uses generated adapters for @JsonClass models.
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter { <init>(...); }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Keep Retrofit service interfaces discoverable by reflection.
-keep,allowobfuscation,allowshrinking interface com.example.data.api.F1ApiService

# Preserve useful coroutine exception stack traces.
-keepattributes Exceptions,InnerClasses,Signature
