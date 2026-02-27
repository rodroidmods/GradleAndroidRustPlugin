package com.examplemobilekmp.rustrodroid

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }

    fun rustGreet(): String {
        return try {
            RustBridge.rustGreeting(platform.name)
        } catch (e: Exception) {
            "Rust not available: ${e.message}"
        }
    }

    fun fibonacci(n: Int): Long {
        return try {
            RustBridge.fibonacci(n)
        } catch (e: Exception) {
            -1L
        }
    }
}