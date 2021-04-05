plugins {
  kotlin("multiplatform") version Versions.Kotlin
  kotlin("plugin.serialization") version Versions.KotlinxSerialization
}

kotlin {
  jvm()
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":echo"))
        api(Deps.Kotlinx.SerializationJson)
      }
    }
  }
}
