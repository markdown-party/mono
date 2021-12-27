plugins {
  kotlin(Plugins.KotlinJvm)
  kotlin(Plugins.KotlinSerialization)
  application
}

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor-server"))
    implementation(project(":echo-transport"))
    implementation(project(":markdown"))

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.test)

    // SL4J
    implementation(libs.slf4j.simple)
  }

  sourceSets { all { languageSettings.optIn("kotlin.RequiresOptIn") } }
}

application {
  group = "markdown.party"
  version = "0.1.0-SNAPSHOT"
  mainClass.set("io.github.alexandrepiveteau.markdown.backend.Main")
}
