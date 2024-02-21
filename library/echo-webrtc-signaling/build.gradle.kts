plugins {
  kotlin(Plugins.KotlinMultiplatform)
  kotlin(Plugins.KotlinSerialization)
  id(Plugins.KotlinBinaryCompatibility)
  id(Plugins.Dokka)
}

kotlin {
  explicitApi()
  jvm { withJava() }
  js(IR) { browser() }

  sourceSets {
    val commonMain by getting { dependencies { api(libs.serialization.core) } }
    val jvmMain by getting
    val jsMain by getting
  }
}
