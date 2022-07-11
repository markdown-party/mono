plugins {
  kotlin(Plugins.KotlinJvm)
  kotlin(Plugins.KotlinSerialization)
  application
}

kotlin {
  dependencies {
    implementation(project(":library:echo"))
    implementation(project(":library:echo-ktor-server"))
    implementation(project(":library:echo-ktor-websockets"))
    implementation(project(":markdown:markdown"))
    implementation(project(":markdown:markdown-signaling"))

    // Kotlinx Immutable collections
    implementation(libs.immutablecollections)

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.test)

    // SL4J
    implementation(libs.slf4j.simple)

    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit"))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
  }

  sourceSets { all { languageSettings.optIn("kotlin.RequiresOptIn") } }
}

application {
  group = "markdown.party"
  version = "0.1.0-SNAPSHOT"
  mainClass.set("party.markdown.Main")
}
