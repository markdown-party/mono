plugins { kotlin(Plugins.KotlinJvm) }

kotlin {
  target { compilations.all { kotlinOptions.allWarningsAsErrors = false } }

  dependencies {
    api(project(":echo"))
    api(project(":echo-transport"))

    // Ktor dependencies.
    api(Deps.Ktor.ServerCore)
    api(Deps.Ktor.ServerWebsockets)
  }

  sourceSets { all { languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn") } }
}
