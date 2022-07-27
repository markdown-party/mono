plugins {
  kotlin(Plugins.KotlinMultiplatform)
  id(Plugins.KotlinBinaryCompatibility)
  id(Plugins.Dokka)
}

kotlin {
  explicitApi()
  jvm()
  js(IR) { browser() }

  targets.all { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":library:echo"))
        api(project(":library:echo-ktor-websockets"))

        // Ktor.
        api(libs.ktor.client.core)
        api(libs.ktor.client.websockets)
      }
    }
    val jvmMain by getting
    val jsMain by getting

    all {
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.optIn("kotlinx.coroutines.FlowPreview")
    }
  }
}
