pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
    plugins {
        id("fabric-loom") version "1.17.11"
    }
}

rootProject.name = "noctra"
