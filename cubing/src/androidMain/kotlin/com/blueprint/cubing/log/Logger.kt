package com.blueprint.cubing.log

import android.util.Log

actual object Logger {
    actual fun log(tag: String, message: String) {
        Log.d(tag, message)
    }
}