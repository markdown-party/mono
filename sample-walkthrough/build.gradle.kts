plugins {
  kotlin(Plugins.KotlinJvm)
  kotlin(Plugins.KotlinSerialization)
}

repositories { mavenCentral() }

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor-client"))
    implementation(project(":echo-ktor-server"))
    implementation(project(":echo-transport"))

    // Ktor
    implementation(Deps.Ktor.ClientEngineCIO)
    implementation(Deps.Ktor.ServerEngineCIO)

    // SL4J
    implementation(Deps.Slf4f.Simple)
  }
}
