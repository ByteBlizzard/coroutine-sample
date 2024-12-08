plugins {
    kotlin("jvm") version "2.0.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.vertx:vertx-web-client:5.0.0.CR2")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.1")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("org.slf4j:slf4j-api:2.0.15")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}