# Keep DeviceAdminReceiver and its XML metadata bindings
-keep public class * extends android.app.admin.DeviceAdminReceiver {
    public <init>();
    public void onReceive(android.content.Context, android.content.Intent);
}

# Keep ContentProvider entry points for inter-user IPC
-keep public class * extends android.content.ContentProvider {
    public <init>();
}

# Keep Activity and Application subclasses
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Jetpack Compose and Coroutines optimization defaults
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
