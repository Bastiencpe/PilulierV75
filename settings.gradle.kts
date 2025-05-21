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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // ← nécessaire pour MPAndroidChart
    }
}

rootProject.name = "Pilulier"

// Modules inclus
include(":app")

// OpenCV SDK local
val openCvSdkDir = file("OpenCV-android-sdk/sdk")
if (openCvSdkDir.exists()) {
    include(":opencv")
    project(":opencv").projectDir = openCvSdkDir
} else {
    println("⚠️ OpenCV SDK non trouvé à: ${openCvSdkDir.absolutePath}")
}
