plugins { kotlin(Plugins.KotlinJvm) }

kotlin {
  target { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  dependencies {
    api(project(":echo"))
    api(project(":echo-transport"))

    // Ktor dependencies.
    api(libs.ktor.server.core)
    api(libs.ktor.server.websockets)
  }

  sourceSets { all { languageSettings.optIn("kotlin.RequiresOptIn") } }
}
