plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  jvm {
    compilations.all { kotlinOptions.jvmTarget = "1.8" }
    testRuns["test"].executionTask.configure { useJUnit() }
    withJava()
  }
  js(IR) {
    browser()
    binaries.executable()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        api(project(":library:echo"))
        api(project(":library:echo-ktor-websockets"))
        api(libs.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        implementation(libs.coroutines.test)
      }
    }
    val jvmTest by getting { dependencies { implementation(kotlin("test-junit")) } }
    val jsMain by getting
    val jsTest by getting { dependencies { implementation(kotlin("test-js")) } }

    all {
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
  }
}
