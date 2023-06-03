plugins {
    kotlin("jvm") version "1.8.21"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    application
}

group = "eu.jameshamilton"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.guardsquare:proguard-core:9.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")

    implementation("com.github.netomi.bat:common:e0d9f969e4f8ca7c95186bf4d2b11ed327bec6e3")
    implementation("com.github.netomi.bat:dexfile:e0d9f969e4f8ca7c95186bf4d2b11ed327bec6e3")
    implementation("com.github.netomi.bat:dexdump:e0d9f969e4f8ca7c95186bf4d2b11ed327bec6e3")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.6.2")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
    testImplementation("io.kotest:kotest-property-jvm:5.6.2")
    testImplementation("io.kotest:kotest-framework-datatest:5.6.2")
    testImplementation("io.mockk:mockk:1.12.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("eu.jameshamilton.bf.MainKt")
}

ktlint {
    enableExperimentalRules.set(true)
    disabledRules.set(setOf("no-wildcard-imports", "experimental:argument-list-wrapping"))
}

tasks.register<Jar>("fatJar") {
    archiveFileName.set("bf.jar")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "eu.jameshamilton.bf.MainKt"
        attributes["Implementation-Version"] = project.version
    }
}

tasks.register<Copy>("copyJar") {
    from(tasks.named("fatJar"))
    into("$rootDir/lib")
}

tasks.named("build") {
    finalizedBy(":copyJar")
}

tasks.named("clean") {
    doFirst {
        File("$rootDir/lib/bf.jar").delete()
    }
}
