object Versions {
  const val Kotlin = "1.4.32"
  const val KotlinBinaryCompatibility = "0.5.0"
  const val KotlinxCoroutines = "1.4.3"
  const val KotlinxImmutableCollections = "0.3.4"
  const val KotlinxSerialization = "1.4.30"
  const val KotlinxSerializationJson = "1.1.0"
  const val Ktor = "1.5.3"

  const val CashAppTurbine = "0.4.1"
  const val Slf4j = "1.7.21"
}

object Plugins {
  const val KotlinMultiplatform = "multiplatform"
  const val KotlinJvm = "jvm"

  const val KotlinBinaryCompatibility = "org.jetbrains.kotlinx.binary-compatibility-validator"
  const val KotlinSerialization = "plugin.serialization"
}

object Deps {
  object Kotlinx {

    const val CoroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KotlinxCoroutines}"

    const val CoroutinesTest =
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.KotlinxCoroutines}"

    const val ImmutableCollections =
        "org.jetbrains.kotlinx:kotlinx-collections-immutable:${Versions.KotlinxImmutableCollections}"

    const val SerializationJson =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinxSerializationJson}"
  }

  object Ktor {
    const val ClientCore = "io.ktor:ktor-client-core:${Versions.Ktor}"
    const val ClientWebsockets = "io.ktor:ktor-client-websockets:${Versions.Ktor}"
    const val ClientEngineCIO = "io.ktor:ktor-client-cio:${Versions.Ktor}"

    const val ServerCore = "io.ktor:ktor-server-core:${Versions.Ktor}"
    const val ServerWebsockets = "io.ktor:ktor-websockets:${Versions.Ktor}"
    const val ServerEngineCIO = "io.ktor:ktor-server-cio:${Versions.Ktor}"
    const val ServerEngineNetty = "io.ktor:ktor-server-netty:${Versions.Ktor}"

    const val ServerEngineTest = "io.ktor:ktor-server-test-host:${Versions.Ktor}"
  }

  object Slf4f {
    const val Simple = "org.slf4j:slf4j-simple:${Versions.Slf4j}"
  }

  object CashApp {
    const val Turbine = "app.cash.turbine:turbine:${Versions.CashAppTurbine}"
  }
}
