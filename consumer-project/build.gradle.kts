import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.tasks.GenerateJavaScriptTask

buildscript {
    dependencies {
        classpath(files("src/teavmc-classpath/resources"))
    }
}

plugins {
    id("java")
    id("eclipse")
    id("org.teavm") version "0.9.2"
}

group = "me.cire3"
version = "1.0-SNAPSHOT"

sourceSets {
    named("main") {
        java.srcDirs(
            "src/main/java",
            "src/teavm/java"
        )
    }
    val platformTwo by creating {
        java.srcDirs(
            "src/main/java",
            "src/platform-two/java"
        )
    }
}

repositories {
    mavenCentral()
}

fun DependencyHandler.platformTwoImplementation(dep: Any): Dependency? {
    return add("platformTwoImplementation", dep)
}

dependencies {
    implementation("org.teavm:teavm-core:0.9.2")
    implementation("org.teavm:teavm-jso:0.9.2")
    implementation("org.teavm:teavm-jso-apis:0.9.2")

    // modified guava
    implementation("me.cire3:modified-guava")

    // platform-two
    platformTwoImplementation("me.cire3:modified-guava")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// dsl version
teavm.js {
    obfuscated = true
    sourceMap = false
    targetFileName = "../classes.js"
    optimization = OptimizationLevel.AGGRESSIVE
    outOfProcess = false
    fastGlobalAnalysis = false
    processMemory = 2048
    entryPointName.set("main")
    mainClass = "me.cire3.EntryPoint"
    outputDir = file("javascript")
    properties = null
    debugInformation = false
}

// i tried to use a custom task instead of dsl, still didn't work
tasks.register<GenerateJavaScriptTask>("generateMyJavaScript") {
    dependsOn("jar")

    group = "replication"

    obfuscated = true
    sourceMap = false
    targetFileName = "classes.js"
    optimization = OptimizationLevel.AGGRESSIVE
    outOfProcess = false
    fastGlobalAnalysis = false
    processMemory = 2048
    entryPointName.set("main")
    mainClass = "me.cire3.EntryPoint"
    outputDir = file("javascript")
    properties = null
    debugInformation = false

    classpath = files(layout.buildDirectory.dir("classes/java/main")) +
            project.configurations.getByName("teavmClasspath")

    println(">>> TEAVM classpath contains:")
    classpath.files.forEach { println("    " + it) }
}
