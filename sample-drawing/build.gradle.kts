plugins {
  kotlin("jvm") version Versions.Kotlin
  kotlin("plugin.serialization") version Versions.KotlinxSerialization
  id("org.jetbrains.compose") version "0.4.0-build179"
  application
}

repositories {
  jcenter()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
  dependencies {
    implementation(project(":echo"))
    implementation(project(":echo-ktor"))
    implementation(project(":echo-ktor-server"))
    implementation(project(":echo-transport"))

    // Immutable collections
    implementation(Deps.Kotlinx.ImmutableCollections)

    // Ktor
    implementation("io.ktor:ktor-server-netty:1.5.3")
    testImplementation("io.ktor:ktor-server-test-host:1.4.3")

    // SL4J
    implementation("org.slf4j:slf4j-simple:1.7.21")

    // Jetpack compose.
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
  }
}
