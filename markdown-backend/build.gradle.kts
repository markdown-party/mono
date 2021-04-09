plugins {
  kotlin("jvm") version Versions.Kotlin
  kotlin("plugin.serialization") version Versions.KotlinxSerialization
  application
}

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor-client"))
    implementation(project(":echo-ktor-server"))
    implementation(project(":echo-transport"))

    // Ktor
    implementation("io.ktor:ktor-server-netty:1.5.3")
    testImplementation("io.ktor:ktor-server-test-host:1.4.3")

    // SL4J
    implementation("org.slf4j:slf4j-simple:1.7.21")
  }
}
