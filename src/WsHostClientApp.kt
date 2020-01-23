import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.features.websocket.WebSockets
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

object WsHostClientApp {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val client = HttpClient(CIO).config { install(WebSockets) }
            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/game/create") {
                send(Frame.Text("who"))
                var  lastMsg = ""
                while (true){
                    incoming.consumeEach { frame ->
                        if(frame is Frame.Text) {
                            println("Server said: ${frame.readText()}")
                            processMessage(frame.readText(),this)
                        }
                    }

                }
            }
        }
    }

    suspend fun processMessage(msg : String, ws : WebSocketSession) {
        if(msg == "!rndStart") {
            ws.send(Frame.Text("rndStart"))
        }
    }
}
