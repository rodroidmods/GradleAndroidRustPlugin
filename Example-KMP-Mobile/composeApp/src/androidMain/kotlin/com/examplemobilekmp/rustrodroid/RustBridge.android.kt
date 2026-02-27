package com.examplemobilekmp.rustrodroid

actual object RustBridge {
    init {
        System.loadLibrary("rustandroid")
    }

    @JvmStatic
    actual external fun rustGreeting(name: String): String

    @JvmStatic
    actual external fun fibonacci(n: Int): Long
}
