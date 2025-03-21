import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    java
    kotlin("jvm") version "1.5.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    maven("https://repo.mattstudios.me/artifactory/public/")
}

dependencies {
    implementation("me.mattstudios:triumph-config:1.0.5-SNAPSHOT")
    compileOnly("com.zaxxer:HikariCP:2.4.0")
    compileOnly("org.xerial:sqlite-jdbc:3.34.0")
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    compileOnly("mysql:mysql-connector-java:5.1.28")
    compileOnly("com.github.limework:redisbungee:0.6.3")
}

group = "me.starmism"
version = "2.0.1-DEV"
description = "BungeeAdminTools Reborn"
java.sourceCompatibility = JavaVersion.VERSION_16

tasks {
    processResources {
        filesNotMatching("**/*.zip") {
            expand("version" to project.version)
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        val translationsFolder = rootProject.file("lang")
        sourceSets.main.get().resources.srcDirs.forEach { resourcesFolder ->
            val destinationZip = File(resourcesFolder, "translations.zip")
            destinationZip.outputStream().use { dest ->
                ZipOutputStream(dest).use { zip ->
                    translationsFolder.listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { translationFile -> translationFile.transferTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    withType<ShadowJar> {
        //minimize()
        archiveFileName.set("${project.name}-${project.version}.jar")
        exclude("META-INF/**")
        exclude("DebugProbesKt.bin")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "16"
    }
}