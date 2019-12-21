package com.birdbrain.components

import com.birdbrain.models.Player
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.atomicfu.atomic
import java.util.concurrent.ConcurrentHashMap

class GameRoom(roomName : String, val hostDisplay : WebSocketSession) {
    val playerList = HashMap<String, Player>()
    private var players = atomic(0)


    fun getPlayerCount() : Int{
        return players.value
    }

    suspend fun addPlayer(player : Player) {
        playerList[player.id] = player
        players.incrementAndGet()
        updatePlayers()
    }

    suspend fun removePlayer(id: String) {
        playerList.remove(id)
        players.decrementAndGet()
        updatePlayers()
    }

    private suspend fun updatePlayers() {
            hostDisplay.send(Frame.Text("Current players: ${getPlayerCount()}"))
        playerList.values.forEach{ player ->
            player.ws?.send(Frame.Text("Current players: ${getPlayerCount()}"))
        }
}
    }