rootProject.name = "vng-graph-layouter"

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
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        maven(url = "https://jogamp.org/deployment/maven/")
        maven(url = "https://ojrepo.soldin.de/")
    }
}