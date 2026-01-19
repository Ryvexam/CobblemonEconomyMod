plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("maven-publish")
    kotlin("jvm") version "1.9.22"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
    maven {
        name = "ImpactDev"
        url = uri("https://maven.impactdev.net/repository/maven-public/")
    }
    maven {
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        name = "Nucleoid"
        url = uri("https://maven.nucleoid.xyz/")
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // SQLite - We don't include it anymore to avoid native conflicts on Mac
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    
    // Sgui for Server-side GUIs
    modImplementation("eu.pb4:sgui:1.6.1+1.21.1")
    include("eu.pb4:sgui:1.6.1+1.21.1")

    // Cobblemon from Modrinth
    modImplementation("maven.modrinth:cobblemon:1.7.1")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
