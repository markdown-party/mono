import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

buildscript {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    classpath("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

plugins {
  id(Plugins.KotlinBenchmark) version Versions.KotlinBenchmark apply false
  id(Plugins.KotlinBinaryCompatibility) version Versions.KotlinBinaryCompatibility apply false
  id(Plugins.Compose) version Versions.Compose apply false
}

// Move the `yarn.lock` file for Kotlin/JS to the `./gradle` directory.
plugins.withType<YarnPlugin> {
  the<YarnRootExtension>().lockFileDirectory = rootDir.resolve("gradle")
}
