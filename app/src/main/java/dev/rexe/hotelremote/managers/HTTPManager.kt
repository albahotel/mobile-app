package dev.rexe.hotelremote.managers

import okhttp3.OkHttpClient
import okhttp3.WebSocket

object HTTPManager {
    var serverAddress: String = "10.65.158.59:8000"

    val client: OkHttpClient = OkHttpClient()
}