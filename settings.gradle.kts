pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}


// Sanitize local.properties to strip deprecated ndk.dir written by CI systems like F-Droid.
// AGP uses android.ndkVersion in build.gradle.kts and warns with [CXX5106] when ndk.dir is present.
listOf(file("local.properties"), file("app/local.properties")).forEach { propFile ->
    if (propFile.exists()) {
        val lines = propFile.readLines()
        if (lines.any { it.trim().startsWith("ndk.dir=") }) {
            propFile.writeText(lines.filterNot { it.trim().startsWith("ndk.dir=") }.joinToString("\n") + "\n")
        }
    }
}

rootProject.name = "LinuxOnAndroid"
include(":app")
