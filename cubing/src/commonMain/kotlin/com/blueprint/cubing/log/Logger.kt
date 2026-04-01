package com.blueprint.cubing.log

expect object Logger {
    fun log(tag: String, message: String)
}