plugins {
    application
    kotlin("jvm") version "1.5.21"
}

application {
     mainClass.set("me.pavi2410.buildserver.AppKt")
}

repositories {
    mavenCentral()
}

dependencies {
    val ktor_version = "1.6.5"
    val logback_version = "1.2.6"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")
    implementation("org.json:json:20210307")

//    testImplementation "io.ktor:ktor-server-tests:$ktor_version"
//    testImplementation "org.jetbrains.kotlin:kotlin-test"
}