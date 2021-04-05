object Versions {
  const val Kotlin = "1.4.32"
  const val KotlinxCoroutines = "1.4.3"
  const val KotlinxImmutableCollections = "0.3.4"
}

object Deps {
  object Kotlinx {

    const val CoroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KotlinxCoroutines}"

    const val CoroutinesTest =
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.KotlinxCoroutines}"

    const val ImmutableCollections =
        "org.jetbrains.kotlinx:kotlinx-collections-immutable:${Versions.KotlinxImmutableCollections}"
  }
}
