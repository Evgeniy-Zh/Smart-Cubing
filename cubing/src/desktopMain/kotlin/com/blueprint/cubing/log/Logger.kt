package com.blueprint.cubing.log

/**
 * Created on 09.03.2026.
 */
actual object Logger {
    actual fun log(tag: String, message: String) {
        println("$tag: $message")
    }
}