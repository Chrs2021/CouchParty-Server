package com.birdbrain

import com.birdbrain.components.ProtoGameRoom
import com.birdbrain.implementation.gametype.DemoGameType
import com.birdbrain.models.Player
import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.request.path
import io.ktor.util.InternalAPI
import io.ktor.util.generateNonce
import kotlinx.coroutines.channels.*
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@InternalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val gameRooms = ConcurrentHashMap<String, ProtoGameRoom>()


    install(Sessions) {
        cookie<Player>("SESSION")
    }


    install(CallLogging)

    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<Player>() == null) {
            call.sessions.set(Player(generateNonce()))
        }
    }

    install(WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {

        webSocket("/game/create") {
            // First of all we get the session.
            var id = generateNonce()
            val session = call.sessions.get<Player>()
            session?.ws = this
            //make seconds a parameter to the socket request
            gameRooms.put(id, DemoGameType(id, session?.ws,60))

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        gameRooms[id]?.onHostMessageRecieved(frame.readText())
                    }
                    if (frame is Frame.Pong) {
                        println("received Pong")
                    }
                    if(frame is Frame.Close) {
                        println("Host leaving reason: ${frame.readReason()}")
                    }
                }
            }catch (ex : Exception) {
                ex.printStackTrace()
                //burn it down!
                for (player in gameRooms[id]?.getPlayers()?.values!!) {
                    println("Kicking ${player.id}")
                    player.ws?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Display Host Left"))
                }
                gameRooms.remove(id)
            }
        }

        webSocket("/game/join/*") {
            val id = call.request.path().split('/').last()
            if(gameRooms.keys.contains(id)){
            val session = call.sessions.get<Player>()

            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                return@webSocket
            }

            println("player ${session?.id} joined")
            session?.ws = this
            gameRooms[id]?.addPlayer(session!!)
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text ) {
                        gameRooms[id]?.onMessageReceived(frame.readText(), session)
                    }
                }
            }catch (ex : Exception){
              ex.printStackTrace()
            } finally{
                session?.id?.let {
                    log.debug("player $id left the room!")
                    gameRooms[id]?.removePlayer(it) }
                }
            }else {
              close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No such room id"))
            }
        }
    }
}

