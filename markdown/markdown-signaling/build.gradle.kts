plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  jvm { withJava() }
  js(IR) {
    browser()
    binaries.executable()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        api(libs.coroutines.core.common)
        api(libs.serialization.core)
      }
    }
  }
}
