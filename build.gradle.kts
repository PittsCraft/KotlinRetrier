import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("maven-publish")
    kotlin("jvm") version "1.9.10"
    application
}

group = "com.pittscraft"
version = "0.0.1-alpha.1"

val coroutinesVersion = "1.7.3"

repositories {
    mavenCentral()
}

publishing {
    repositories {
        mavenCentral()
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.pittscraft"
            artifactId = "kotlin-retrier"
            version = "0.0.1-alpha.1"

            from(components["java"])
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    )
}

application {
    mainClass.set("MainKt")
}