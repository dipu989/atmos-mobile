package dev.atmos.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

// "android" or "ios" — used when registering the device with the push backend.
expect val platformId: String
