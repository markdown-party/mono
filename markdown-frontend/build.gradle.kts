plugins { kotlin(Plugins.KotlinJs) }

repositories {
  jcenter()
}

// TODO : Reference these versions in Versions.kt
dependencies {
  implementation(kotlin("stdlib-js"))

  implementation(project(":echo"))
  implementation(project(":echo-ktor-client"))
  implementation(project(":echo-transport"))

  implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")

  implementation("org.jetbrains:kotlin-react:16.13.1-pre.110-kotlin-1.4.0")
  implementation("org.jetbrains:kotlin-react-dom:16.13.1-pre.110-kotlin-1.4.0")
  implementation("org.jetbrains:kotlin-react-router-dom:5.1.2-pre.110-kotlin-1.4.0")
  implementation(npm("react", "16.13.0"))
  implementation(npm("react-dom", "16.13.0"))
}

kotlin { js { browser() } }
