plugins {
  kotlin(Plugins.KotlinJvm)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor-client"))
    implementation(project(":echo-ktor-server"))
    implementation(project(":echo-transport"))

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.cio)

    // SL4J
    implementation(libs.slf4j.simple)
  }
}
