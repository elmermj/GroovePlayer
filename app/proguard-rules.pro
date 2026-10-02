# Release R8 rules for the app module. Library AARs also contribute consumer rules;
# those are called out here when this file does not repeat them.

# Crashlytics deobfuscation. The mapping file still has the original names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Moshi KotlinJsonAdapterFactory and Retrofit read annotations and generic signatures.
# kotlin-reflect also ships -keepattributes RuntimeVisible*Annotations and -keep class kotlin.Metadata.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

# --- Room ---
# Room.databaseBuilder loads GroovePlayerDatabase_Impl by that exact name.
# room-runtime also keeps every RoomDatabase subclass. Converters is called from
# generated code; PrivilegeTier.name is stored in SQLite, so the enum constants stay.
-keep class com.aethelworks.grooveplayer.data.local.db.GroovePlayerDatabase { *; }
-keep class com.aethelworks.grooveplayer.data.local.db.GroovePlayerDatabase_Impl { <init>(...); }
-keep class com.aethelworks.grooveplayer.data.local.db.Converters { *; }

# Enum.name / valueOf is persisted (Room privilege tier, transfer status, repeat mode,
# restore-swap journal). R8 must not rename the constants.
-keepclassmembers enum com.aethelworks.grooveplayer.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# --- Retrofit / OkHttp / Moshi models ---
# NetworkModule uses Moshi's reflection factory (KotlinJsonAdapterFactory), not codegen.
# It requires @Metadata, primary constructors (including Kotlin default-value overloads),
# and @param:Json names. OkHttp and Retrofit ship their own consumer rules
# (PublicSuffixDatabase, service interfaces, suspend Continuation).
-keep class com.aethelworks.grooveplayer.data.remote.dto.** {
    <init>(...);
    <fields>;
    <methods>;
}
-keep class com.aethelworks.grooveplayer.data.remote.api.** { *; }

# --- Media3 / ExoPlayer ---
# media3-exoplayer, media3-extractor, media3-datasource, and media3-ui keep the
# renderer, extractor, and data-source constructors that those factories load by name.
# No app rule: this module does not add its own reflective player classes.

# --- Hilt ---
# hilt-android keeps @EntryPoint types. The Hilt Gradle plugin keeps generated injectors.
# Manifest components below keep the @AndroidEntryPoint classes the system constructs.

# --- Google sign-in / Credential Manager ---
# credentials-play-services-auth keeps CredentialProviderPlayServicesImpl when
# CredentialManager is present. play-services-basement keeps Parcelable CREATORs.
# GoogleIdTokenCredential is referenced directly from GoogleIdTokenProvider.

# --- Play Billing ---
# billing keeps the Play AIDL types and ProxyBillingActivity. No app billing models
# are loaded by reflection; verify/ack bodies are the Moshi DTOs above.

# --- Firebase ---
# firebase-components keeps ComponentRegistrar. Crashlytics line numbers are the
# SourceFile,LineNumberTable attributes above.

# --- WorkManager ---
# This module has no Worker subclass. AdMob registers OfflineNotificationPoster,
# OfflinePingSender, and WorkManagerUtil, which WorkManager constructs by class name.
# work-runtime and play-services-ads-lite ship the same keeps; repeat them so a
# transitive copy cannot drop the constructors.
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
-keep class androidx.work.WorkerParameters

# --- Manifest entry points (widget, playback, wear bridge) ---
# aapt also keeps these. The system instantiates them by the manifest class name.
-keep class com.aethelworks.grooveplayer.GroovePlayerApp { <init>(); }
-keep class com.aethelworks.grooveplayer.MainActivity { <init>(); }
-keep class com.aethelworks.grooveplayer.widget.PlaybackWidgetProvider { <init>(); }
-keep class com.aethelworks.grooveplayer.services.MusicPlaybackService { <init>(); }
-keep class com.aethelworks.grooveplayer.services.ShareTransferService { <init>(); }
-keep class com.aethelworks.grooveplayer.services.NearbyTransferService { <init>(); }
-keep class com.aethelworks.grooveplayer.services.LibraryImportService { <init>(); }
-keep class com.aethelworks.grooveplayer.wear.GroovePlayerWearListenerService { <init>(); }

# --- App reflection ---
# jaudiotagger loads org.jaudiotagger.tag.id3.framebody.FrameBody<ID> with Class.forName
# and copies tags through constructors. It has no consumer rules. The jar also ships
# org.jaudiotagger.test, which references Swing; that package is not kept.
# java.awt / ImageIO are desktop-only references inside the library.
-keep class org.jaudiotagger.audio.** { *; }
-keep class org.jaudiotagger.tag.** { *; }
-keep class org.jaudiotagger.logging.** { *; }
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# BluetoothRepositoryImpl and NfcShareDiscovery reflect on Android framework methods
# (BluetoothA2dp.connect, NfcAdapter.setNdefPushMessage). Those classes are platform
# types, not app types.
