plugins {
  kotlin(Plugins.KotlinJs)
  id(Plugins.KotlinBinaryCompatibility)
}

kotlin {
  js(IR) { browser() }

  dependencies {
    api(project(":library:echo"))
    implementation(project(":library:echo-webrtc-signaling"))

    // Kotlinx serialization
    implementation(libs.serialization.json)

    // Ktor dependencies
    api(libs.ktor.client.core)

    // Kotlin Wrappers
    implementation("org.jetbrains.kotlin-wrappers:kotlin-webrtc:0.0.32-pre.343")

    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit"))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
  }
}
