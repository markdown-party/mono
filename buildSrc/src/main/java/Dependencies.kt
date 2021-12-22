object Versions {
  const val Kotlin = "1.6.10"
  const val KotlinBinaryCompatibility = "0.8.0"
  const val KotlinxCoroutines = "1.6.0"
  const val KotlinxImmutableCollections = "0.3.4"
  const val KotlinxSerialization = "1.6.10"
  const val KotlinxSerializationFormat = "1.3.1"
  const val KotlinxDateTime = "0.3.1"
  const val Ktor = "1.6.7"

  const val CashAppTurbine = "0.7.0"
  const val Slf4j = "1.7.21"

  const val Compose = "1.0.1-rc2"
}

object Plugins {
  const val KotlinMultiplatform = "multiplatform"
  const val KotlinJvm = "jvm"
  const val KotlinJs = "js"

  const val KotlinBinaryCompatibility = "org.jetbrains.kotlinx.binary-compatibility-validator"
  const val KotlinSerialization = "plugin.serialization"

  const val Compose = "org.jetbrains.compose"
}

object Deps {
  object Kotlinx {

    const val CoroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KotlinxCoroutines}"

    const val CoroutinesCoreJs =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:${Versions.KotlinxCoroutines}"

    const val CoroutinesTest =
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.KotlinxCoroutines}"

    const val DateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${Versions.KotlinxDateTime}"

    const val ImmutableCollections =
        "org.jetbrains.kotlinx:kotlinx-collections-immutable:${Versions.KotlinxImmutableCollections}"

    const val SerializationCore =
        "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.KotlinxSerializationFormat}"

    const val SerializationProtobuf =
        "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${Versions.KotlinxSerializationFormat}"

    const val SerializationJson =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinxSerializationFormat}"
  }

  object Ktor {
    const val CommonHttpCIO = "io.ktor:ktor-http-cio:${Versions.Ktor}"

    const val ClientCore = "io.ktor:ktor-client-core:${Versions.Ktor}"
    const val ClientWebsockets = "io.ktor:ktor-client-websockets:${Versions.Ktor}"
    const val ClientEngineCIO = "io.ktor:ktor-client-cio:${Versions.Ktor}"
    const val ClientEngineJs = "io.ktor:ktor-client-js:${Versions.Ktor}"

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
