plugins {
  kotlin(Plugins.KotlinJvm)
  kotlin(Plugins.KotlinSerialization)
  id(Plugins.Compose)
}

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor-client"))
    implementation(project(":echo-ktor-server"))
    implementation(project(":echo-transport"))

    // Immutable collections
    implementation(libs.immutablecollections)

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.test)

    // SL4J
    implementation(libs.slf4j.simple)

    // Jetpack compose.
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
  }
}

compose.desktop {
  application { mainClass = "io.github.alexandrepiveteau.echo.samples.drawing.Main" }
}
