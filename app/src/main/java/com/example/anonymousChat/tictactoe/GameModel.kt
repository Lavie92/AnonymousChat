package com.example.anonymousChat.tictactoe


data class GameModel (
    var gameId: String = "",
    var filledPos: MutableList<String> = MutableList(9) { "" },
    var winner: String = "",
    var gameStatus: GameStatus = GameStatus.CREATED,
    var currentPlayer: String = "X",
    var player1Id: String? = null,
    var player2Id: String? = null,
    var waitingForOpponent: Boolean = true
)


enum class GameStatus{
    PLAYER_EXITED,
    CREATED,
    JOINED,
    INPROGRESS,
    FINISHED
}