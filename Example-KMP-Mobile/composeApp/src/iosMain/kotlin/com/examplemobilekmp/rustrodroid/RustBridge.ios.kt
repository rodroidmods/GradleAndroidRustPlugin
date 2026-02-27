package com.examplemobilekmp.rustrodroid

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import rustios.fibonacci_c
import rustios.rust_greeting_c
import rustios.rust_greeting_free

@OptIn(ExperimentalForeignApi::class)
actual object RustBridge {
    actual fun rustGreeting(name: String): String {
        val result = rust_greeting_c(name)
        val greeting = result?.toKString() ?: "Unknown"
        rust_greeting_free(result)
        return greeting
    }

    actual fun fibonacci(n: Int): Long {
        return fibonacci_c(n.toUInt()).toLong()
    }
}
