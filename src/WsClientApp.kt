import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.features.websocket.WebSockets
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

object WsClientApp {
    @JvmStatic
    fun main(args: Array<String>) {

        runBlocking {
            var lastCommand  = ""
            val client = HttpClient(CIO).config { install(WebSockets) }
            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/game/join/d5128a863930c7b5") {
                send(Frame.Text("who"))

                send(Frame.Text("pNick:${(Math.random()*10000).toLong().toString(16)}"))

                while (true){
                    if(!lastCommand.isNullOrBlank()) {
                        send(Frame.Text(lastCommand))
                        lastCommand = ""
                    }

                    incoming.consumeEach { frame ->
                        if(frame is Frame.Text) {
                            println("Server said: ${frame.readText()}")
                        }
                    }

                    lastCommand = readLine().toString()
                }
            }
        }
    }
}
