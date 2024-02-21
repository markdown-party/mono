plugins {
  kotlin(Plugins.KotlinJvm)
  id(Plugins.KotlinBinaryCompatibility)
  id(Plugins.Dokka)
}

kotlin {
  explicitApi()
  target { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  dependencies {
    api(project(":library:echo"))
    api(project(":library:echo-ktor-websockets"))

    // Ktor dependencies.
    api(libs.ktor.server.core)
    api(libs.ktor.server.websockets)
  }

  sourceSets { all { languageSettings.optIn("kotlin.RequiresOptIn") } }
}
