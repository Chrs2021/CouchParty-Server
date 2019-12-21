package com.birdbrain

import com.birdbrain.components.GameRoom
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

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@InternalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val gameRooms = HashMap<String, GameRoom>()
    install(Sessions) {
        cookie<Player>("SESSION")
    }
    install(CallLogging)

    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<Player>() == null) {
            call.sessions.set(Player(generateNonce().toString()))
        }
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/game/create") {
            // First of all we get the session.
            var id = generateNonce().toString()

            gameRooms[id.toString()] = GameRoom(id.toString(), this)
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        when (frame.readText()) {
                            "who" -> send(Frame.Text("gameroom ID: $id"))
                            "path" -> send(Frame.Text(call.request.path()))
                            "count" -> gameRooms[id]?.hostDisplay?.send(Frame.Text("test"))
                            "bye" -> this.close(CloseReason(CloseReason.Codes.NORMAL, "Said Bye Bye"))
                            else -> send(Frame.Text("Unknown Message ${frame.readText()}"))
                        }
                    }
                }
            }finally {
                gameRooms.remove(id)
            }
        }
        webSocket("/game/join/*") {
            val id = call.request.path().split('/').last()
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
                        receiveMessage(frame.readText(),session)
                    }
                }
            }finally {
                session?.id?.let {
                    println("player $it left")
                    gameRooms[id]?.removePlayer(it) }
            }
        }
    }

}

suspend fun receiveMessage(readText: String, session: Player) {
    when (readText) {
        "who" -> session.ws?.send(Frame.Text("Player id: ${session?.id.toString()}"))
        "setPlayer" -> session?.displayName = "test"
        "setAvatar" -> session?.avatar
        else -> println("invalid argument: $readText")
    }
}

