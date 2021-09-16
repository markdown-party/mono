plugins { kotlin(Plugins.KotlinMultiplatform) }

kotlin {
  jvm()
  js(IR) { browser() }

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
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.optIn("kotlinx.coroutines.FlowPreview")
    }
  }
}
