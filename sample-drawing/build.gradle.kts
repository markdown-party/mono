plugins {
  kotlin(Plugins.KotlinJvm)
  kotlin(Plugins.KotlinSerialization)
  id("org.jetbrains.compose") version "0.4.0-build179"
}

repositories {
  jcenter()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor-client"))
    implementation(project(":echo-ktor-server"))
    implementation(project(":echo-transport"))

    // Immutable collections
    implementation(Deps.Kotlinx.ImmutableCollections)

    // Ktor
    implementation(Deps.Ktor.ClientEngineCIO)
    implementation(Deps.Ktor.ServerEngineCIO)
    testImplementation(Deps.Ktor.ServerEngineTest)

    // SL4J
    implementation(Deps.Slf4f.Simple)

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
