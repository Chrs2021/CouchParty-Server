package com.birdbrain.ui.awt


import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.close
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.text.Caret

class AwtClientInterface(x : Int, y : Int, width : Int, height : Int, title: String, ws : WebSocketSession)   {
    val mainFram = Frame()
    val messageArea = TextArea()


    init {
        mainFram.setSize(640,480)
        val textField = TextField()
        val sendButton = Button("S")
        sendButton.addActionListener {
            runBlocking {
                ws.send(io.ktor.http.cio.websocket.Frame.Text(textField.text.toString()))
            }
            addMessage("${textField.text}\n")
            textField.text = ""
        }

        mainFram.layout = GridLayout(3,2)
        mainFram.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                mainFram.dispose()
                runBlocking {
                    ws.close( CloseReason(CloseReason.Codes.GOING_AWAY, "Goodbye!"))
                }
            }
        })
        mainFram.add(messageArea)
        mainFram.add(textField)
        mainFram.add(sendButton)
        mainFram.isVisible = true
    }

    public fun flashWindow () {
        mainFram.title = "Current King!"
        mainFram.run { requestFocus() }
    }

    public fun addMessage(msg : String) {
        messageArea.text += msg
        messageArea.caretPosition = messageArea.text.length
    }
}