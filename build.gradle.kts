buildscript {
  repositories {
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

allprojects {
  repositories {
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}
