import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

group = "space.solevarnya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    ksp(project(":casket-processor"))
    implementation(project(":casket-core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}