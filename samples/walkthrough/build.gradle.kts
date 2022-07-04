plugins {
  kotlin(Plugins.KotlinJvm)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  dependencies {
    implementation(project(":library:echo"))
    implementation(project(":library:echo-ktor-client"))
    implementation(project(":library:echo-ktor-server"))
    implementation(project(":library:echo-transport"))

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.cio)

    // SL4J
    implementation(libs.slf4j.simple)
  }
}
