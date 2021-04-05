plugins {
  kotlin("multiplatform") version Versions.Kotlin
  kotlin("plugin.serialization") version Versions.KotlinxSerialization
}

kotlin {
  jvm()
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":echo"))
        implementation(Deps.Kotlinx.SerializationJson)
      }
    }
  }
}
