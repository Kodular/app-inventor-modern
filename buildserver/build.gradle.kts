plugins {
    application
    kotlin("jvm") version "1.4.21"
}

application {
     mainClass.set("me.pavi2410.buildserver.ServerKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("io.ktor:ktor-server-core:1.5.1")
    implementation("io.ktor:ktor-server-netty:1.5.1")
    implementation("ch.qos.logback:logback-classic:1.2.3")
//    implementation("io.ktor:ktor-serialization:1.5.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlin_serialization")

//    testImplementation "io.ktor:ktor-server-tests:$ktor_version"
}