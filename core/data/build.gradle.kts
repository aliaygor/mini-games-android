plugins { id("com.android.library"); kotlin("android") }
android { namespace = "com.thirtyseconds.data"; compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions { isCoreLibraryDesugaringEnabled = true; sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(project(":core:domain")); implementation("androidx.datastore:datastore-preferences:1.1.7"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") }
