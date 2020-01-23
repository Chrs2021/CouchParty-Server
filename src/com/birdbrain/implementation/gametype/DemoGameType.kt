package com.birdbrain.implementation.gametype

import com.birdbrain.components.ProtoGameRoom
import com.birdbrain.models.Answer
import com.birdbrain.models.Player
import com.birdbrain.models.SelectedWinner
import com.google.gson.GsonBuilder
import io.ktor.http.cio.websocket.WebSocketSession
import io.nayuki.qrcodegen.QrCode
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.math.min

class DemoGameType(var roomName: String, val hostDisplayConn: WebSocketSession?, val roundTimerSeconds: Int) :
	ProtoGameRoom(roomName, hostDisplayConn, roundTimerSeconds, 3) {
	val host = "labs.snapvids.com"
	lateinit var king: String
	lateinit var chosenWord: String
	var answerMap: LinkedHashMap<String, String> = LinkedHashMap()
	lateinit var roundTimerEvent: Timer
	lateinit var timerPosition: AtomicInt
	lateinit var roundTimerTask: TimerTask
	var remainingRoundTime = -1
	val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()


	init {
		val joinCode = QrCode.encodeText("https://$host/game/$roomName", QrCode.Ecc.MEDIUM).toImage(10, 2)
		var baos = ByteArrayOutputStream()
		ImageIO.write(joinCode, "png", baos)
		runBlocking {
			broadcastHost("qr:${Base64.getEncoder().encode(baos.toByteArray()).toString(Charset.defaultCharset())}")
		}
		println("Spawning $roomName")
	}

	private suspend fun startGame() {
        if(!::king.isInitialized)
            chooseKing()
		timerPosition = atomic(roundTimerSeconds)
		gameStage = 1
		broadcastHost("gamestage: $gameStage")
        announceKing()
		broadcastAll("selectingText")
        broadcastPlayers("king'd!", king)
		broadcastPlayers("chooseWord", king)
		buildTimer()
	}

	private suspend fun announceKing() {
		val players = getPlayers()
		players[king]?.isKing = true
		val playerValues = players.values.toMutableList()
		val playerListing = gson.toJson(playerValues)
		broadcastHost("{\"pys\" : ${playerListing}}")
	}

	private fun timerTick() = runBlocking {
		broadcastAll(gson.toJson(com.birdbrain.models.Timer(timerPosition.decrementAndGet(), roundTimerSeconds)))
		if (timerPosition.value == 0) {
			roundTimerEvent.cancel()
			if (gameStage in  1..2) {
				announceGameStage(-1)
				resetKing()
			}

		}
	}

	private fun buildTimer() {
		timerPosition.value = roundTimerSeconds
		roundTimerEvent = Timer()
		roundTimerEvent.scheduleAtFixedRate(object : TimerTask() {
			override fun run() {
				timerTick()
			}

		}, 250, 1000)

	}

	private fun chooseKing() {
		var players = getPlayers().keys.toList()
		val randNum = Math.abs((Math.random() * 4283 % players.size)).toInt()
		king = players[randNum]
	}

	//Client Processing
	private suspend fun processSetupCommands(data: String, playerId: Player) {}

	private suspend fun processGameSetupCommands(data: String, playerId: Player) {
		if (data.startsWith("gStart") && playerId.isLeader) {
			broadcastHost("gStart")
			startGame()
		}
	}

	private suspend fun processGameConfigCommands(data: String, playerId: Player) {
		if (data.startsWith("chWord:") && playerId.id == king) {
			chosenWord = data.split(':')[1].trimStart().trimEnd()
			if (data.split(':')[1].isNotEmpty()) {
				roundTimerEvent.cancel()
				broadcastHost("chWord: $chosenWord")
				startPlayRound()
			}
		}
	}

	private suspend fun processGameAnswers(data: String, playerId: Player) {
		val answer = gson.fromJson<Answer>(data, Answer::class.java)
		if (!answerMap.containsKey(playerId.id) && !playerId.isKing) {
			if (answer.answer.trim().length > 0)
				answerMap[playerId.id] = answer.answer
			val answerValues = answerMap.values.toMutableList()
			val answerList = gson.toJson(answerValues)
			broadcastHost("{\"aws\" : ${answerList}}")
            if(receivedAllAnswers()) {
                announceGameStage(3)
	            roundTimerEvent.cancel()
                broadcastHost("answersIn")
                broadcastPlayers(gson.toJson(answerMap), king)
            }
		}
	}

    private fun receivedAllAnswers(): Boolean {
        var received = false
        var unansweredPlayers = (getPlayers().clone() as HashMap<String, Player>).keys
		unansweredPlayers.remove(king)
	    answerMap.keys.forEach {
            it->
                unansweredPlayers.remove(it)
        }

        if(unansweredPlayers.size < 1) {
            received = true
        }

        return received
    }

	private suspend fun resetKing() {
		getPlayers()[king]?.isKing = false
		king = ""
		updatePlayers()
	}

    private suspend fun decidingResults(data: String, playerId: Player) {
		if (playerId.isKing) {
           val winner = gson.fromJson<SelectedWinner>(data, SelectedWinner::class.java)
            if (getPlayers().containsKey(winner?.winningId)) {
                broadcastHost(data)
                updateKing(winner.winningId)
                announceKing()
                announceGameStage(0)
            }
		}
	}

    private fun updateKing(winner: String) {
        val players  = getPlayers()
        players[king]?.isKing = false
        king = ""
        for (player in players){

            if (player.key == winner) {
                player.value.isKing = true
                king = player.key
            }
        }
    }

    //Host Processing

	private suspend fun processGameSetupHostCommands(data: String) {

	}

	private suspend fun processGameConfigHostCommands(data: String) {
		if (data == "rndStart") {
			buildTimer()
			announceGameStage(2)
		}
	}

	private suspend fun processGameHostAnswers(data: String) {

	}

	private suspend fun decidingHostResults(data: String) {

	}

	private suspend fun startPlayRound() {
		answerMap.clear()
		broadcastHost("!rndStart")
	}


	//TODO: build synchronization code for the tv display to kick off timers after animation effects

	override suspend fun removePlayer(id: String) {
		super.removePlayer(id)
		getPlayers().values.first().isLeader = true
		resetKing()
		if(getPlayers().count() < minPlayers) {
			roundTimerEvent.cancel()
		}
	}
	override suspend fun onHostMessageRecieved(data: String) {
		println("Host DEBUG: got data: $data")
		when (data) {
			"who" -> broadcastHost("${this.roomName}, ${getPlayers().values}")
		}

		when (gameStage) {
			0 -> processGameSetupHostCommands(data)
			1 -> processGameConfigHostCommands(data)
			2 -> processGameHostAnswers(data)
			3 -> decidingHostResults(data)
		}
	}

	override suspend fun onMessageReceived(data: String, playerId: Player) {
		println("Client DEBUG: got data: $data")

		//look at prototype component for definition of gameStage
		when (gameStage) {
			-1 -> processSetupCommands(data, playerId)
			0 -> processGameSetupCommands(data, playerId)
			1 -> processGameConfigCommands(data, playerId)
			2 -> processGameAnswers(data, playerId)
			3 -> decidingResults(data, playerId)
		}


		if (data.startsWith("pNick:")) {
			val newName = data.split(':')[1]
			if (newName.isNotBlank()) {
				playerId.displayName = newName

				val playerValues = getPlayers().values.toMutableList()
				val playerListing = gson.toJson(playerValues)
				broadcastHost("{\"pys\" : ${playerListing}}")
			}
		}

		when (data) {
			"who" -> {
				val playerData = HashMap<String, Player>()
				playerData[roomName] = playerId
				broadcastPlayers(gson.toJson(playerData), playerId.id)
			}
		}
	}

}
