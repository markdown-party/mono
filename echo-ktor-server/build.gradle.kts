plugins { kotlin("jvm") version Versions.Kotlin }

kotlin {
  target { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  dependencies {
    api(project(":echo"))
    api(project(":echo-transport"))

    // Ktor dependencies.
    api(Deps.Ktor.ServerCore)
    api(Deps.Ktor.ServerWebsockets)
  }

  sourceSets { all { languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn") } }
}
