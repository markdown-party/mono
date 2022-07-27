plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
  id(Plugins.KotlinAllOpen)
  id(Plugins.KotlinBenchmark)
  id(Plugins.KotlinBinaryCompatibility)
  id(Plugins.Dokka)
}

kotlin {
  jvm {
    compilations.all { kotlinOptions.jvmTarget = "1.8" }
    testRuns["test"].executionTask.configure { useJUnit() }
    withJava()
  }

  js(IR) { browser() }

  targets.all { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        api(project(":library:echo-core"))
        api(libs.coroutines.core.common)
        api(libs.serialization.core)
        implementation(libs.serialization.protobuf)
        implementation(libs.immutablecollections)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))

        implementation(libs.coroutines.test)
        implementation(libs.turbine)
      }
    }
    val jvmMain by getting
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.coroutines.test)
      }
    }
    val jvmBenchmarks by creating {
      dependsOn(commonMain)
      dependencies { implementation(libs.benchmark) }
    }
    val jsMain by getting
    val jsTest by getting { dependencies { implementation(kotlin("test-js")) } }
    all {
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("kotlin.time.ExperimentalTime")
      languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
      languageSettings.optIn("kotlinx.coroutines.FlowPreview")
    }
  }
  targets { jvm { compilations.create("benchmarks") } }
}

benchmark { targets { register("jvmBenchmarks") } }

allOpen { annotation("org.openjdk.jmh.annotations.State") }
