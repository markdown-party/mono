plugins { kotlin(Plugins.KotlinJs) }

repositories {
  jcenter()
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib-js"))

  implementation(project(":echo-ktor-client"))
  implementation(project(":echo-transport"))
  implementation(project(":markdown"))

  implementation(Deps.Ktor.ClientEngineJs)
  implementation(Deps.Kotlinx.CoroutinesCoreJs)
  implementation(Deps.Kotlinx.DateTime)

  // TODO : Reference these versions in Versions.kt
  implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")

  implementation("org.jetbrains.kotlin-wrappers:kotlin-react:17.0.2-pre.203-kotlin-1.5.0")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:17.0.2-pre.203-kotlin-1.5.0")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom:5.2.0-pre.203-kotlin-1.5.0")
  implementation(npm("react", "17.0.2"))
  implementation(npm("react-dom", "17.0.2"))
  implementation(npm("react-router-dom", "5.2.0"))

  // CodeMirror.
  implementation(npm("@codemirror/lang-markdown", "0.18.4"))
  implementation(npm("@codemirror/basic-setup", "0.18.2"))
}

kotlin { js(IR) { browser { binaries.executable() } } }
