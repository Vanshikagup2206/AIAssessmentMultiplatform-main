package com.vanshika.multiplatformproject

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform