plugins { id("com.android.application"); kotlin("android"); kotlin("kapt"); id("com.google.dagger.hilt.android"); id("org.jetbrains.kotlin.plugin.compose") }
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) { apply(plugin = "com.google.gms.google-services"); apply(plugin = "com.google.firebase.crashlytics") }
android {
    namespace = "com.thirtyseconds.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.thirtyseconds.minigames"
        buildConfigField("String", "COMPETITION_URL", "\"${providers.gradleProperty("COMPETITION_URL").getOrElse("")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "0.1.0"
        manifestPlaceholders["admobAppId"] = providers.gradleProperty("ADMOB_APP_ID").getOrElse("ca-app-pub-3940256099942544~3347511713")
        buildConfigField("String", "REWARDED_ID", "\"${providers.gradleProperty("ADMOB_REWARDED_ID").getOrElse("ca-app-pub-3940256099942544/5224354917")}\"")
        buildConfigField("String", "INTERSTITIAL_ID", "\"${providers.gradleProperty("ADMOB_INTERSTITIAL_ID").getOrElse("ca-app-pub-3940256099942544/1033173712")}\"")
    }
    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
    }
    bundle { language { enableSplit = false } }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { isCoreLibraryDesugaringEnabled = true; sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation("com.google.dagger:hilt-android:2.57.1")
    kapt("com.google.dagger:hilt-compiler:2.57.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation(project(":core:domain")); implementation(project(":core:data")); implementation(project(":core:platform"))
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.ui:ui"); implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3"); debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
}
