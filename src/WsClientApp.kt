import com.birdbrain.ui.awt.AwtClientInterface
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.features.websocket.WebSockets
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

object WsClientApp {
    private var running = true
    @JvmStatic
    fun main(args : Array<String>) {

        //build a config hashmap to configure the client
        var config = HashMap<String,String>()
        args.forEach { arg ->
            val kvp = arg.split("=")
            if(kvp.size > 1) {
                config[kvp[0].toLowerCase()] = kvp[1]
            }
        }

        if(config.containsKey("host") && config.containsKey("player") && config.containsKey("room")) {
            startClient(config["host"], config["player"], config["room"])
        } else {
            println("Test client utility")
            println("Usage: <app name> host=HostName player=PlayerName room=RoomCode")
        }

    }

    fun startClient(host : String?, playerName : String?, roomCode : String?) {
        runBlocking {
            val client = HttpClient(CIO).config { install(WebSockets) }
            client.ws(method = HttpMethod.Get, host = "$host", port = 8089, path = "/game/join/${roomCode}") {
                val window  = AwtClientInterface(0,0,80, 80, "Test", this)

                window.mainFram.addWindowListener(object : WindowAdapter(){
                    override fun windowClosed(e: WindowEvent?) {
                        running = false
                    }
                })
                send(Frame.Text("who"))
                send(Frame.Text("pNick:${playerName}"))

                while (running){
                    incoming.consumeEach { frame ->
                        if(frame is Frame.Text) {
                          if(frame.readText().equals("king'd!")) {
                              window.flashWindow()
                          }
                          window.addMessage("Server said: ${frame.readText()}\n")
                        }
                    }
                }
            }
        }
    }
}
