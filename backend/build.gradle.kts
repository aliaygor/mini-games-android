plugins { kotlin("jvm"); application }
java { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
application { mainClass.set("com.thirtyseconds.server.ServerKt") }
dependencies {
    implementation(project(":core:domain"))
    implementation("com.google.firebase:firebase-admin:9.4.3")
    implementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
}
