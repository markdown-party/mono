plugins {
  kotlin("multiplatform") version Versions.Kotlin
  kotlin("plugin.serialization") version Versions.KotlinxSerialization
}

kotlin {
  jvm()

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
