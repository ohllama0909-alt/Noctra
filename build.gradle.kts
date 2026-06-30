plugins {
    id("fabric-loom")
}

val modVersion: String = property("mod_version") as String
val mcVersion: String = property("minecraft_version") as String
val yarnMappings: String = property("yarn_mappings") as String
val loaderVersion: String = property("loader_version") as String
val fabricApiVersion: String = property("fabric_version") as String

group = "de.Snenjih"
version = modVersion

base {
    archivesName.set("mandatory")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.lenni0451.net/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
    include(implementation("net.raphimc:MinecraftAuth:4.1.1")!!)
}

loom {
    accessWidenerPath.set(rootProject.file("src/main/resources/mandatory.accesswidener"))
    @Suppress("UnstableApiUsage")
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName.set("mandatory.refmap.json")
    }
}

tasks.processResources {
    val props = mapOf(
        "id"        to "mandatory",
        "version"   to modVersion,
        "minecraft" to mcVersion,
        "loader"    to loaderVersion
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.jar {
    from("LICENSE")
    manifest {
        attributes(
            "Implementation-Title"   to "mandatory",
            "Implementation-Version" to modVersion
        )
    }
}
