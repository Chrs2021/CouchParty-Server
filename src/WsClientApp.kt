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
        val name = "John"
        println(name)
        runBlocking {
            var lastCommand  = ""
            val client = HttpClient(CIO).config { install(WebSockets) }
            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/game/join/d7b466db1b6b1d75") {
                send(Frame.Text("who"))
                var running = true
                send(Frame.Text("pNick:${name}"))

                while (running){
                    if(!lastCommand.isNullOrBlank()) {
                        send(Frame.Text(lastCommand))
                        lastCommand = ""
                    }

                    incoming.consumeEach { frame ->
                        if(frame is Frame.Text) {
                            println("Server said: ${frame.readText()}")
                        }

                        if (frame is Frame.Close) {
                            running = false
                            println("got removed: ${frame.readReason()}")
                        }
                    }

                    lastCommand = readLine().toString()
                }
            }
        }
    }
}
