plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://repo.mattstudios.me/artifactory/public/")
}

dependencies {
    implementation("com.zaxxer:HikariCP:2.4.0")
    implementation("me.mattstudios:triumph-config:1.0.5-SNAPSHOT")
    implementation("org.slf4j:slf4j-log4j12:1.7.2")
    implementation("org.slf4j:slf4j-api:1.7.2")
    implementation("log4j:log4j:1.2.17")
    compileOnly("net.md-5:bungeecord-api:1.15-SNAPSHOT")
    compileOnly("mysql:mysql-connector-java:5.1.28")
    compileOnly("org.xerial:sqlite-jdbc:3.7.15-M1")
    compileOnly("com.github.limework:redisbungee:0.6.3")
}

group = "fr.Alphart"
version = "1.6.4"
description = "BungeeAdminTools"
java.sourceCompatibility = JavaVersion.VERSION_16

tasks {
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        minimize()
        archiveFileName.set(rootProject.name + ".jar")
        exclude("META-INF/**")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}