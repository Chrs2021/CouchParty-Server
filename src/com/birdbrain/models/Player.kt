package com.birdbrain.models

import io.ktor.http.cio.websocket.WebSocketSession

data class Player (val id : String, var displayName: String = "", var avatar : String ="", var ws : WebSocketSession? = null)