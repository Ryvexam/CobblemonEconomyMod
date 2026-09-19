plugins {
    id("fabric-loom") version "1.16.2"
    id("maven-publish")
    kotlin("jvm") version "2.3.20"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
    maven {
        name = "ImpactDev"
        url = uri("https://maven.impactdev.net/repository/development/")
    }
    maven {
        name = "Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        url = uri("https://cursemaven.com")
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

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    include("org.xerial:sqlite-jdbc:3.45.1.0")
    
    // Sgui for Server-side GUIs
    modImplementation("eu.pb4:sgui:1.6.1+1.21.1")
    include("eu.pb4:sgui:1.6.1+1.21.1")

    // Cobblemon Fabric from Modrinth. The project/version IDs are intentional:
    // using the human version number alone resolves the NeoForge artifact.
    modImplementation("maven.modrinth:MdwFAVRL:${project.property("cobblemon_modrinth_version")}")
    
    // YAWP Integration
    modApi("curse.maven:yawp-663276:6176022")
    modImplementation(files("libs/yawp-1.21.1-fabric-0.6.3-beta1.jar"))

    // Impactor API (compileOnly - only used when Impactor is present at runtime)
    // Transitive deps excluded because net.kyori:event-api:5.0.0-SNAPSHOT isn't publicly available
    compileOnly("net.impactdev.impactor.api:economy:5.3.0") { isTransitive = false }
    compileOnly("net.impactdev.impactor.api:core:5.3.0") { isTransitive = false }
    compileOnly("net.impactdev.impactor.api:storage:5.3.0") { isTransitive = false }
    // Adventure API needed by Impactor's interfaces (provided by Impactor at runtime)
    compileOnly("net.kyori:adventure-api:4.17.0")
    // Maven artifact version class referenced by Impactor's PluginMetadata
    compileOnly("org.apache.maven:maven-artifact:3.9.0")
    // Kyori event bus used by Impactor's API (5.0.0-SNAPSHOT extracted from Impactor jar,
    // not published to any public Maven repository)
    compileOnly(files("libs/event-api-5.0.0-SNAPSHOT.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
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

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
