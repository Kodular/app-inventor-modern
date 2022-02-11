package me.pavi2410.buildserver

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/buildserver") {
            get("/health") {

            }
        }
        get("/") {
            call.respondText("Hello World!")
        }
    }
}