plugins { kotlin("jvm") version Versions.Kotlin }

kotlin {
  dependencies {
    api(project(":echo"))
    api(project(":echo-transport"))
    api("io.ktor:ktor-server-core:1.5.3")
    api("io.ktor:ktor-websockets:1.5.3")
  }
}
