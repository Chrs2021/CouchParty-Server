package com.birdbrain.interfaces

import com.birdbrain.models.Player

interface GameController {
    suspend fun onHostMessageRecieved(data: String)
    suspend fun onMessageReceived(data : String, playerId : Player)
}