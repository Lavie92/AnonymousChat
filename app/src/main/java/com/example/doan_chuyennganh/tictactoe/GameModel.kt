package com.example.doan_chuyennganh.tictactoe

import kotlin.random.Random

data class GameModel(
    var gameId: String = "",
    var filledPos: MutableList<String> = MutableList(9) { "" },
    var winner: String = "",
    var gameStatus: GameStatus = GameStatus.CREATED,
    var currentPlayer: String = if (Random.nextBoolean()) "X" else "O",
    var player1Id: String? = null,
    var player2Id: String? = null,
    var currentUserIsPlayer1: Boolean = false,
    var waitingForOpponent: Boolean = true
)

enum class GameStatus {
    CREATED, JOINED, INPROGRESS, FINISHED
}
