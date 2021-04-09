plugins { kotlin("multiplatform") version Versions.Kotlin }

kotlin {
  jvm()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":echo"))
        api(project(":echo-transport"))
        api("io.ktor:ktor-client-core:1.5.3")
        api("io.ktor:ktor-client-cio:1.5.3")
        api("io.ktor:ktor-client-websockets:1.5.3")
      }
    }
  }
}
