plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
}

kotlin {
  jvm() { withJava() }
  js(IR) { browser() }

  sourceSets {
    val commonMain by getting { dependencies { api(libs.serialization.core) } }
    val jvmMain by getting
    val jsMain by getting
  }
}
