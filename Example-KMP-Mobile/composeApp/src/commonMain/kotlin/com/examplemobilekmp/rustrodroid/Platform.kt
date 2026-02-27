package com.examplemobilekmp.rustrodroid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform