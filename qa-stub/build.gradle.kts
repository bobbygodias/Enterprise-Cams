plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "org.enterprisecams.fixture"
    compileSdk = 35
    defaultConfig {
        // Isolated emulator fixture, NOT the official Yoosee binary.
        applicationId = "com.yoosee"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "qa-only"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
