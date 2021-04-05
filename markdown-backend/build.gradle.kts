plugins {
  kotlin("jvm") version Versions.Kotlin
  application
}

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor"))
    implementation(project(":echo-transport"))
  }
}
