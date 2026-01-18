pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "ImpactDev"
            url = uri("https://maven.impactdev.net/repository/maven-public/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "cobblemon-economy"
