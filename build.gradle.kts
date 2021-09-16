buildscript {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.Kotlin}")
    classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.KotlinxSerialization}")
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
  id(Plugins.KotlinBinaryCompatibility) version Versions.KotlinBinaryCompatibility apply false
  id(Plugins.Compose) version Versions.Compose apply false
}
