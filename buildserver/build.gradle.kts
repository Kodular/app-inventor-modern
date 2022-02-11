plugins {
    application
    kotlin("jvm") version "1.5.31"
}

application {
     mainClass.set("me.pavi2410.buildserver.AppKt")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        name = "ktor-eap"
    }
}

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation:2.0.0-eap-256")
    implementation("io.ktor:ktor-server-core:2.0.0-eap-256")
    implementation("io.ktor:ktor-server-netty:2.0.0-eap-256")
    val ktor_version = "1.6.5"
    val logback_version = "1.2.6"
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")
    implementation("org.json:json:20210307")

//    testImplementation "io.ktor:ktor-server-tests:$ktor_version"
//    testImplementation "org.jetbrains.kotlin:kotlin-test"
}