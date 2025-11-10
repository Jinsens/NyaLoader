plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    // id("com.google.devtools.ksp")  // 切换到KAPT以提高稳定性
    id("kotlin-kapt")                 // ← 使用KAPT处理注解
    
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nyapass.loader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nyapass.loader"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // 启用R8代码优化和混淆
            isMinifyEnabled = true
            // 启用资源缩减
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug版本不启用混淆，方便调试
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true  // 启用 BuildConfig 生成
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android - 更新到最新版本
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.11.0")

    // Compose - 更新BOM到最新版本
    implementation(platform("androidx.compose:compose-bom:2025.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel - 更新到最新版本
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    // Room - 更新到最新版本
    val roomVersion = "2.8.3"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")  // 使用KAPT处理Room注解

    // OkHttp - 保持稳定版本
    implementation("com.squareup.okhttp3:okhttp:5.3.0")

    // Coroutines - 更新到最新版本
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    
    // Kotlinx Serialization - JSON序列化支持
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // WorkManager - 更新到最新版本
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    // Gson - 更新到最新版本
    implementation("com.google.code.gson:gson:2.13.2")

    // Navigation - 更新到最新版本
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // Firebase - Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    // Add other Firebase products as needed
    // https://firebase.google.com/docs/android/setup#available-libraries

    // guava - 更新到最新版本
    implementation("com.google.guava:guava:33.5.0-android")
    
    // Testing - 更新到最新版本
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.11.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
