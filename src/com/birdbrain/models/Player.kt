package com.birdbrain.models

import com.google.gson.annotations.Expose
import io.ktor.http.cio.websocket.WebSocketSession

data class Player (
    @Expose
    val id : String,
    @Expose
    var displayName: String = "",
    @Expose
    var avatar : String ="",
    @Expose
    var isKing : Boolean = false,
    var ws : WebSocketSession? = null)