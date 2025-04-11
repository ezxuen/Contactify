plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ezxuen.contactify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ezxuen.contactify"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.vision.common)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.play.services.mlkit.text.recognition)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.google.mlkit:text-recognition:16.0.0") // for OCR
    implementation("com.google.mlkit:entity-extraction:16.0.0-beta5") // for OCR
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("com.google.android.material:material:1.11.0")

}