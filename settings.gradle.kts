pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // lielugit-updater : dépôt Maven public de la release, versionné dans libs/ (ni jeton ni secret).
        maven {
            url = uri("$rootDir/libs/lielugit-maven")
            content { includeGroup("com.lielu") }
        }
    }
}

rootProject.name = "Yakwa"
include(":app")
