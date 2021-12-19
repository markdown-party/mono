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
    implementation(Deps.Ktor.ClientEngineCIO)
    implementation(Deps.Ktor.ServerEngineCIO)
    testImplementation(Deps.Ktor.ServerEngineTest)

    // SL4J
    implementation(Deps.Slf4f.Simple)
  }

  sourceSets { all { languageSettings.optIn("kotlin.RequiresOptIn") } }
}

application {
  group = "markdown.party"
  version = "0.1.0-SNAPSHOT"
  mainClass.set("io.github.alexandrepiveteau.markdown.backend.Main")
}
