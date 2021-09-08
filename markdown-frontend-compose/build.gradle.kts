plugins {
  kotlin(Plugins.KotlinMultiplatform)
  id("org.jetbrains.compose") version "1.0.0-alpha4-build321"
}

repositories {
  jcenter()
  mavenCentral()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
  js(IR) {
    browser()
    binaries.executable()
  }

  sourceSets {
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))

        implementation(project(":echo-ktor-client"))
        implementation(project(":echo-transport"))
        implementation(project(":markdown"))

        implementation(Deps.Ktor.ClientEngineJs)
        implementation(Deps.Kotlinx.CoroutinesCoreJs)
        implementation(Deps.Kotlinx.DateTime)

        // CodeMirror.
        implementation(npm("@codemirror/lang-markdown", "0.18.4"))
        implementation(npm("@codemirror/basic-setup", "0.18.2"))
        implementation(npm("@codemirror/tooltip", "0.18.4"))
        implementation(npm("@codemirror/theme-one-dark", "0.18.1"))

        // Jetpack Compose
        implementation(compose.web.core)
        implementation(compose.runtime)
      }
    }
  }
}
