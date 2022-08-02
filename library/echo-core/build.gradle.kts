plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
  id(Plugins.KotlinAllOpen)
  id(Plugins.KotlinBenchmark)
  id(Plugins.KotlinBinaryCompatibility)
  id(Plugins.Dokka)
}

kotlin {
  explicitApi()

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
        api(libs.serialization.core)
        api(libs.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmMain by getting
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
      languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
  }
  targets { jvm { compilations.create("benchmarks") } }
}

benchmark {
  targets { register("jvmBenchmarks") }
  configurations {
    named("main") {
      iterations = Benchmarks.IterationsCount
      iterationTime = Benchmarks.IterationsTime
      iterationTimeUnit = Benchmarks.IterationsTimeUnit
      warmups = Benchmarks.WarmupsCount
    }
  }
}

allOpen { annotation("org.openjdk.jmh.annotations.State") }
