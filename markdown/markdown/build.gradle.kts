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

  js(IR) {
    browser()
    binaries.executable()
  }

  targets.all { compilations.all { kotlinOptions.allWarningsAsErrors = true } }

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
    val jvmBenchmarks by creating {
      dependsOn(commonMain)
      dependencies {
        implementation(libs.benchmark)
        implementation(libs.coroutines.test)
      }
    }
    val jsMain by getting
    val jsTest by getting { dependencies { implementation(kotlin("test-js")) } }

    all {
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }

    targets { jvm { compilations.create("benchmarks") } }
  }
}

benchmark {
  targets { register("jvmBenchmarks") }
  configurations {
    named("main") {
      iterations = Benchmarks.IterationsCount
      iterationTime = Benchmarks.IterationsTime
      iterationTimeUnit = Benchmarks.IterationsTimeUnit
      warmups = Benchmarks.WarmupsCount
      include("treeInsertions")
      // include("treeMoves")
    }
  }
}

allOpen { annotation("org.openjdk.jmh.annotations.State") }
