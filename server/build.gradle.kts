plugins {
    kotlin("jvm") version "2.0.21"
    application
}

application {
    mainClass = "org.example.ServerMainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.vertx:vertx-web:5.0.0.CR2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}