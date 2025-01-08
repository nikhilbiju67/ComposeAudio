package com.nikhilbiju67.audio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform