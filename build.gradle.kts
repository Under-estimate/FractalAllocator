plugins {
    kotlin("jvm") version "1.9.0"
}

group = "org.zrnq"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}