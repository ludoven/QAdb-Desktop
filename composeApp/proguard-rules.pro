# Keep JNA classes and members because JNA uses reflection on internal/private members
# (e.g., com.sun.jna.Native#dispose), which can break after shrinking/obfuscation.
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.platform.** { *; }
-keep class com.sun.jna.ptr.** { *; }
-keep class com.sun.jna.win32.** { *; }

