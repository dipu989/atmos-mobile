package dev.atmos.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
