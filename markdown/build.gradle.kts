plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  jvm()
  js(IR) { browser() }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":echo"))
        api(project(":echo-transport"))
      }
    }
  }
}
