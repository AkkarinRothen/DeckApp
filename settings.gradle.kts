pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Para PdfiumAndroid
    }
}

rootProject.name = "DeckApp"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":feature:library")
include(":feature:deck")
include(":feature:draw")
include(":feature:import")
include(":feature:session")
include(":feature:settings")
include(":feature:tables")
include(":feature:encounters")
include(":feature:npcs")
include(":feature:wiki")
include(":feature:reference")
include(":feature:hexploration")
include(":feature:mythic")
include(":feature:dice")
