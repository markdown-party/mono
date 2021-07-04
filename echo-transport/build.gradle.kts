plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  jvm()
  js(IR) { browser() }

  targets.all { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":echo"))
        api(Deps.Kotlinx.SerializationJson)
        api(Deps.Ktor.CommonHttpCIO)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmTest by getting { dependencies { implementation(kotlin("test-junit")) } }
    val jsTest by getting { dependencies { implementation(kotlin("test-js")) } }

    all {
      languageSettings.useExperimentalAnnotation(
          "kotlinx.serialization.ExperimentalSerializationApi")
    }
  }
}
