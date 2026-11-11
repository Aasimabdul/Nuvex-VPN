plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("io.ktor.plugin") version "3.0.3"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.nuvex"
version = "0.1.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}


dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.0.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.postgresql:postgresql:42.7.4")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.3")
}
kotlin { jvmToolchain(17) }
