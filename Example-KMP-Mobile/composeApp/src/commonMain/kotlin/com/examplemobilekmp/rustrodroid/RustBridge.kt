package com.examplemobilekmp.rustrodroid

expect object RustBridge {
    fun rustGreeting(name: String): String
    fun fibonacci(n: Int): Long
}
