# Project-specific ProGuard / R8 rules for Message App (com.ap.messages)

# Preserve Android Manifest components required for default SMS operations
-keep class com.ap.messages.MainActivity { <init>(...); }
-keep class com.ap.messages.receiver.SmsReceiver { <init>(...); }
-keep class com.ap.messages.receiver.MmsReceiver { <init>(...); }
-keep class com.ap.messages.service.RespondViaMessageService { <init>(...); }
-keep class com.ap.messages.receiver.ScheduledSmsReceiver { <init>(...); }
-keep class com.ap.messages.receiver.ScheduledSmsBootReceiver { <init>(...); }

# Preserve Remote Config enum members used for dynamic configuration mapping
-keepclassmembers enum com.ap.messages.ads.** { *; }

# Preserve Room database and WorkManager implementations instantiated via reflection
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
