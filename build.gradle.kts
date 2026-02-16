import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GitHub

buildscript {
    dependencies {
        classpath("org.kohsuke:github-api:${project.property("github_api_version") as String}")
    }
}

plugins {
    id("maven-publish")
    id("fabric-loom")
    // id("org.ajoberstar.grgit")
    // id("com.matthewprenger.cursegradle")
    // id("com.modrinth.minotaur")
    id("idea")
}

operator fun Project.get(property: String): String {
    return property(property) as String
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

version = project["mod_version"]
group = project["maven_group"]

val environment: Map<String, String> = System.getenv()
val releaseName = "${name.split("-").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }} ${(version as String).split("+")[0]}"
val releaseType = (version as String).split("+")[0].split("-").let { if(it.size > 1) if(it[1] == "BETA" || it[1] == "ALPHA") it[1] else "ALPHA" else "RELEASE" }
val releaseFile = "${layout.buildDirectory.asFile.get()}/libs/${base.archivesName.get()}-${version}.jar"
val cfGameVersion = (version as String).split("+")[1].let{ if(!project["minecraft_version"].contains("-") && project["minecraft_version"].startsWith(it)) project["minecraft_version"] else "$it-Snapshot"}

fun getChangeLog(): String {
    return "A changelog can be found at https://github.com/lucaargolo/$name/commits/"
}

fun getBranch(): String {
    environment["GITHUB_REF"]?.let { branch ->
        return branch.substring(branch.lastIndexOf("/") + 1)
    }
    return "unknown"
}

loom {
    accessWidenerPath.set(file("src/main/resources/seasons.accesswidener"))
}

repositories {
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/releases")
    }
    maven {
        name = "Shedaniel"
        url = uri("https://maven.shedaniel.me/")
    }
    mavenLocal()
}

dependencies {
    minecraft("com.mojang:minecraft:${project["minecraft_version"]}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${project["loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project["fabric_version"]}")

    modRuntimeOnly("com.terraformersmc:modmenu:${project["modmenu_version"]}")
    modRuntimeOnly("me.shedaniel:RoughlyEnoughItems-fabric:${project["rei_version"]}")
}

configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${project["loader_version"]}")
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    inputs.property("version", project.version)

    from(sourceSets["main"].resources.srcDirs) {
        include("fabric.mod.json")
        expand(mutableMapOf("version" to project.version))
    }

    from(sourceSets["main"].resources.srcDirs) {
        exclude("fabric.mod.json")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

java {
    withSourcesJar()
}

tasks.jar {
    from("LICENSE")
}

/*
//Github publishing
tasks.register("github") {
...
}

//Curseforge publishing
curseforge {
...
}

//Modrinth publishing
modrinth {
...
}
tasks.modrinth.configure {
    group = "upload"
}
*/

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.cafeteria.dev/releases")
            credentials {
                username = environment["MAVEN_USERNAME"]
                password = environment["MAVEN_PASSWORD"]
            }
        }


    }
}

/*
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
*/
