package com.birdbrain

import com.birdbrain.components.ProtoGameRoom
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
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {

        webSocket("/game/create") {
            // First of all we get the session.
            var id = generateNonce()

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        gameRooms[id]?.onHostMessageRecieved(frame.readText())
                    }
                }
            }finally {
                gameRooms.remove(id)
            }
        }

        webSocket("/game/join/*") {
            val id = call.request.path().split('/').last()
            if(gameRooms.contains(id)){
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
            }finally {
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

