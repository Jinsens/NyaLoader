plugins {
    id("com.android.application") version "9.0.0" apply false
    id("com.android.library") version "9.0.0" apply false
    // Note: org.jetbrains.kotlin.android is no longer needed with AGP 9.0 built-in Kotlin
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10" apply false
    
    // KSP for faster annotation processing (Room, Hilt)
    id("com.google.devtools.ksp") version "2.3.5" apply false
    
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.4" apply false
    
    // Hilt dependency injection
    id("com.google.dagger.hilt.android") version "2.59.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
