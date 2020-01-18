package com.birdbrain.ui.awt


import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.close
import javafx.scene.input.KeyCode
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.text.Caret

class AwtClientInterface(x : Int, y : Int, width : Int, height : Int, title: String, ws : WebSocketSession)   {
    val mainFram = Frame()
    val messageArea = TextArea()


    init {
        messageArea.isEditable = false
        messageArea.isFocusable = false
        mainFram.setSize(640,480)
        val textField = TextField()
        textField.focusTraversalKeysEnabled = true
        textField.addKeyListener(object : KeyListener{
            override fun keyTyped(ke: KeyEvent?) {
                val keyCode = ke?.keyCode
                if (keyCode!!.equals(KeyCode.ENTER)) {
                    runBlocking {
                        ws.send(io.ktor.http.cio.websocket.Frame.Text(textField.text.toString()))
                    }
                    addMessage("${textField.text}\n")
                    textField.text = ""
                }
            }

            override fun keyPressed(p0: KeyEvent?) {
                //don't care
            }

            override fun keyReleased(ke: KeyEvent?) {

            }

        })
        val sendButton = Button("Send")
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
        textField.requestFocus()
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