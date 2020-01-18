package com.birdbrain.components

import com.birdbrain.interfaces.GameController
import com.birdbrain.models.Player
import com.google.gson.GsonBuilder
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import java.util.concurrent.ConcurrentHashMap

abstract class ProtoGameRoom(roomName : String, val hostDisplay : WebSocketSession?, val roundTimer : Int
                             , val minPlayers : Int) : GameController {
    /*
   Round stage definitions
        -1 = Not started waiting for players,
         0 = Minimium players, in game not started,
         1 = round started waiting for king(word chooser)
         2 = round started waiting on answers
         3 = judging by the king

   Repeat Flow
 */
    var gameStage = -1
    private val playerList = LinkedHashMap<String, Player>()
    private var players = atomic(0)
    private lateinit var currentTimerVal : AtomicInt

    fun getPlayerCount() : Int{
        return players.value
    }

    fun getPlayers() : LinkedHashMap<String, Player> {
        return playerList
    }

    suspend fun addPlayer(player : Player) {
        playerList[player.id] = player
        players.incrementAndGet()
        updatePlayers()
        if(players.value >= minPlayers) {
            gameStage = 0
            broadcastAll("gamestage: $gameStage")
        }
    }

    open suspend fun removePlayer(id: String) {
        playerList.remove(id)
        players.decrementAndGet()
        updatePlayers()
        if (gameStage  > 0  && players.value < minPlayers) {
            gameStage = -1
            broadcastAll("gamestage: $gameStage")
        }
    }

    suspend fun announceGameStage(stage : Int) {
        gameStage = stage
        broadcastAll("gamestage: $gameStage")
    }

    suspend fun broadcastPlayers(msg : String, playerID : String = "0") {
        if(playerID == "0") {
            playerList.values.forEach { player ->
                player.ws?.send(Frame.Text(msg))
            }
        } else {
            playerList[playerID]?.ws?.send(Frame.Text(msg))
        }
    }

    suspend fun broadcastHost(msg : String) {
        hostDisplay?.send(Frame.Text(msg))
    }

    suspend fun broadcastAll(msg : String) {
        broadcastHost(msg)
        broadcastPlayers(msg)
    }

    internal suspend fun updatePlayers() {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val playerValues = getPlayers().values.toMutableList()
        val playerListing = gson.toJson(playerValues)
        broadcastHost("{\"pys\" : ${playerListing}}")

        playerList.values.forEach{ player ->
            player.ws?.send(Frame.Text("Current players: ${getPlayerCount()}"))
        }
    }
}