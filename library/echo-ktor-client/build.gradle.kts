plugins { kotlin(Plugins.KotlinMultiplatform) }

kotlin {
  jvm()
  js(IR) { browser() }

  targets.all { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":library:echo"))
        api(project(":library:echo-transport"))

        // Ktor.
        api(libs.ktor.client.core)
        api(libs.ktor.client.websockets)
      }
    }

    all {
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.optIn("kotlinx.coroutines.FlowPreview")
    }
  }
}
