rootProject.name = "openrndr-template"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven(url = "https://jogamp.org/deployment/maven/")
        maven(url = "https://ojrepo.soldin.de/")
    }
}