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
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
  }
}
