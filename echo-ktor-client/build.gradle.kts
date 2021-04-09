plugins { kotlin("multiplatform") version Versions.Kotlin }

kotlin {
  jvm()

  targets.all { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":echo"))
        api(project(":echo-transport"))

        // Ktor.
        api(Deps.Ktor.ClientCore)
        api(Deps.Ktor.ClientWebsockets)
      }
    }

    all {
      languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
      languageSettings.useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.useExperimentalAnnotation("kotlinx.coroutines.FlowPreview")
    }
  }
}
