plugins { kotlin("multiplatform") version Versions.Kotlin }

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":echo"))
                api(project(":echo-transport"))
            }
        }
    }
}
