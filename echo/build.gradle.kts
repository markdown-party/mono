import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins { kotlin("multiplatform") version Versions.Kotlin }

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    val options = listOf(
      "-Xopt-in=kotlin.RequiresOptIn",            // Used to define custom @OptIn.
      "-Xinline-classes",                         // Reaching Beta stability in KT 1.5
      "-Xopt-in=kotlin.ExperimentalUnsignedTypes" // Unsigned numbers.
    )
    freeCompilerArgs = freeCompilerArgs + options
  }
}

kotlin {
  jvm {
    compilations.all { kotlinOptions.jvmTarget = "1.8" }
    testRuns["test"].executionTask.configure { useJUnit() }
    withJava()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation(Deps.Kotlinx.CoroutinesCore)
        implementation(Deps.Kotlinx.ImmutableCollections)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmMain by getting
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
        implementation(Deps.Kotlinx.CoroutinesTest)
      }
    }
  }
}
