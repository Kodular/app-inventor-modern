package me.pavi2410.buildserver

import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
	embeddedServer(Netty, port = 8000) {
		configureRouting()
	}.start(wait = true)
}