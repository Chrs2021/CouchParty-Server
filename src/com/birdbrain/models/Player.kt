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
    @Expose
    var isLeader : Boolean = false,
    @Expose
    var points : Int = 0,
    var ws : WebSocketSession? = null)