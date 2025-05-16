package dev.rexe.hotelremote

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform