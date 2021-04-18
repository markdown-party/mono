plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  jvm()
  js { browser() }

  targets.all { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":echo"))
        api(Deps.Kotlinx.SerializationJson)
      }
    }

    all { languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes") }
  }
}
