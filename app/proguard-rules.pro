# Keep kotlinx serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    companion object;
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <fields>;
    <methods>;
}
-keep class * implements kotlinx.serialization.KSerializer {
    *;
}
