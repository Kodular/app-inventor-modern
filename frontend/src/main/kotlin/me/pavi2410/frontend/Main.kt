package me.pavi2410.frontend

import react.dom.*
import kotlinx.browser.document

fun main() {
    render(document.getElementById("root")) {
        h1 {
            +"Hello, React+Kotlin/JS!"
        }
    }
}