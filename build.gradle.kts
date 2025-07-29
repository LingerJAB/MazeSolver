import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.10"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.10"
}

group = "io.github. lingerjab"
version = "1.0-SNAPSHOT"
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public/") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(compose.material3)
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    testImplementation(kotlin("test"))
}


compose.desktop {
    application {
        mainClass = "io.github.lingerjab.MazeSolverWindowKt"
        nativeDistributions {
            targetFormats(TargetFormat.AppImage, TargetFormat.)
            packageName = "Maze Solver"
            packageVersion = "1.0.0"
            includeAllModules = false

            windows { iconFile.set(project.file("src/main/resources/favicon.ico")) }

        }
    }
}

kotlin {
    jvmToolchain(21) // Java 21
}