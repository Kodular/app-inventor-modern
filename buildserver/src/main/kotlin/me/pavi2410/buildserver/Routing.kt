package me.pavi2410.buildserver

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

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