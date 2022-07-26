plugins {
  kotlin(Plugins.KotlinJvm)
  id(Plugins.KotlinBinaryCompatibility)
  id(Plugins.Dokka)
}

kotlin {
  dependencies {
    implementation(project(":library:echo"))
    implementation(project(":library:echo-webrtc-signaling"))

    // Ktor dependencies
    api(libs.ktor.server.core)
    api(libs.ktor.server.websockets)

    // Immutable collections
    implementation(libs.immutablecollections)

    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit"))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
  }
}
