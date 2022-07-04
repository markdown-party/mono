import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
  kotlin(Plugins.KotlinMultiplatform)
  id(Plugins.Compose)
}

// FIXME : Remove once KT-48273 is fixed
afterEvaluate {
  rootProject.extensions.configure<NodeJsRootExtension> {
    versions.webpackDevServer.version = "4.0.0"
  }
}

rootProject.plugins.withType(
  org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
  rootProject.the<NodeJsRootExtension>().versions.webpackCli.version = "4.9.0"
}

kotlin {
  js(IR) {
    browser {
      // Enable tailwind configuration
      copy {
        from("./postcss.config.js")
        into("${rootDir}/build/js/packages/kotlin-echo-${project.name}")
      }
      copy {
        from("./tailwind.config.js")
        into("${rootDir}/build/js/packages/kotlin-echo-${project.name}")
      }
      commonWebpackConfig { cssSupport.enabled = true }
    }
    binaries.executable()
  }

  sourceSets {
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))

        implementation(project(":echo-ktor-client"))
        implementation(project(":echo-transport"))
        implementation(project(":markdown"))

        implementation(libs.ktor.client.js)
        implementation(libs.coroutines.core.js)
        implementation(libs.datetime)

        // CodeMirror.
        implementation(npm("@codemirror/lang-markdown", "0.18.4"))
        implementation(npm("@codemirror/basic-setup", "0.18.2"))
        implementation(npm("@codemirror/tooltip", "0.18.4"))
        implementation(npm("@codemirror/theme-one-dark", "0.18.1"))

        // TailwindCSS
        implementation(npm("postcss", "8.3.6"))
        implementation(npm("postcss-loader", "6.1.1"))
        implementation(npm("autoprefixer", "10.3.4"))
        implementation(npm("tailwindcss", "2.2.16"))

        // Kotlin JS wrappers
        implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions:1.0.1-pre.343")
        implementation("org.jetbrains.kotlin-wrappers:kotlin-webrtc:0.0.32-pre.343")

        // Jetpack Compose
        implementation(compose.web.core)
        implementation(compose.runtime)
      }
    }
    all { languageSettings.optIn("kotlin.RequiresOptIn") }
  }
}
