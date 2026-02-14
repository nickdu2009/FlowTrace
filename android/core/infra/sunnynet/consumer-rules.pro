# Keep SunnyNet official JNI entrypoints & DTOs.
-keep class com.SunnyNet.** { *; }

# Keep callback receiver method names/signatures looked up from native.
-keep class com.flowtrace.infra.sunnynet.SunnyNetCallbackReceiver { *; }

