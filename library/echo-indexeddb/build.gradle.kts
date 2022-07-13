plugins {
  kotlin(Plugins.KotlinJs)
  id(Plugins.KotlinBinaryCompatibility)
}

kotlin {
  js(IR) { browser() }

  dependencies {
    api(project(":library:echo"))

    // IndexedDB
    implementation(libs.juul.indexeddb.core)

    // Kotlin Wrappers
    implementation("org.jetbrains.kotlin-wrappers:kotlin-webrtc:0.0.32-pre.343")

    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit"))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
  }
}
