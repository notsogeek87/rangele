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
        // lielugit-updater : GitHub Packages exige un jeton read:packages, lu dans l'environnement
        // (GPR_USER / GPR_TOKEN) ou ~/.gradle/gradle.properties (gpr.user / gpr.token). Jamais dans le dépôt.
        maven {
            url = uri("https://maven.pkg.github.com/notsogeek87/lielugit-updater")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GPR_USER")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GPR_TOKEN")
            }
            content { includeGroup("com.lielu") }
        }
    }
}

rootProject.name = "Yakwa"
include(":app")
