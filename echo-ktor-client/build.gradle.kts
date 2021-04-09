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
        api("io.ktor:ktor-client-core:1.5.3")
        api("io.ktor:ktor-client-websockets:1.5.3")
      }
    }

    all {
      languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
      languageSettings.useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.useExperimentalAnnotation("kotlinx.coroutines.FlowPreview")
    }
  }
}
