package com.example.doan_chuyennganh.tictactoe


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.doan_chuyennganh.databinding.ActivityGameBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class GameActivity : AppCompatActivity(),View.OnClickListener {

    lateinit var binding: ActivityGameBinding

    private var gameModel : GameModel? = null
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.auth = FirebaseAuth.getInstance()

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val gameId = intent.getStringExtra("GAME_ID")
        if (gameId != null) {
            observeGameChanges(gameId)
        } else {
            Toast.makeText(this, "Invalid Game ID", Toast.LENGTH_SHORT).show()
            finish()
        }
        GameData.fetchGameModel()

        binding.btn0.setOnClickListener(this)
        binding.btn1.setOnClickListener(this)
        binding.btn2.setOnClickListener(this)
        binding.btn3.setOnClickListener(this)
        binding.btn4.setOnClickListener(this)
        binding.btn5.setOnClickListener(this)
        binding.btn6.setOnClickListener(this)
        binding.btn7.setOnClickListener(this)
        binding.btn8.setOnClickListener(this)

        binding.startGameBtn.setOnClickListener {
            startGame()
        }

        GameData.gameModel.observe(this){
            gameModel = it
            setUI()
        }
        binding.endGameBtn.setOnClickListener {
            gameModel?.gameId?.let { gameId ->
                FirebaseDatabase.getInstance().getReference("games/$gameId/gameStatus")
                    .setValue(GameStatus.PLAYER_EXITED.name)
            }
            resetScores()
            finish()
        }



    }
    private fun observeGameChanges(gameId: String) {
        FirebaseDatabase.getInstance().getReference("games").child(gameId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val game = snapshot.getValue(GameModel::class.java)
                    game?.let {
                        if (it.gameStatus.name == "PLAYER_EXITED" && it.player2Id != FirebaseAuth.getInstance().currentUser?.uid) {
                            Toast.makeText(this@GameActivity, "The other player has exited the game.", Toast.LENGTH_LONG).show()
                            finish()
                        } else if (it.gameStatus.name == "JOINED" && it.player2Id != null && it.player2Id != FirebaseAuth.getInstance().currentUser?.uid) {
                            Toast.makeText(this@GameActivity, "A new player has joined the game!", Toast.LENGTH_LONG).show()
                        } else {
                            gameModel = it
                            setUI()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GameActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    fun setUI(){
        gameModel?.apply {
            binding.btn0.text = filledPos[0]
            binding.btn1.text = filledPos[1]
            binding.btn2.text = filledPos[2]
            binding.btn3.text = filledPos[3]
            binding.btn4.text = filledPos[4]
            binding.btn5.text = filledPos[5]
            binding.btn6.text = filledPos[6]
            binding.btn7.text = filledPos[7]
            binding.btn8.text = filledPos[8]

            binding.startGameBtn.visibility = View.VISIBLE

            val currentUserIsCreator = FirebaseAuth.getInstance().currentUser?.uid == player1Id

            binding.gameStatusText.text = when (gameStatus) {
                GameStatus.CREATED, GameStatus.JOINED -> "Waiting for game to start."
                GameStatus.INPROGRESS -> "Game in progress."
                GameStatus.FINISHED -> "Game finished."
                else -> ({}).toString()
            }

            // Show start button if game is not yet in progress
            binding.startGameBtn.visibility = if (gameStatus in listOf(GameStatus.CREATED, GameStatus.JOINED)) View.VISIBLE else View.INVISIBLE
            if (gameStatus == GameStatus.FINISHED) {
                val resultMessage = when (winner) {
                    "X" -> "Player X wins!"
                    "O" -> "Player O wins!"
                    "Draw" -> "It's a draw!"
                    else -> "Game finished!"
                }
                Toast.makeText(applicationContext, resultMessage, Toast.LENGTH_LONG).show()
                binding.startGameBtn.visibility = if (gameStatus in listOf(GameStatus.CREATED, GameStatus.JOINED, GameStatus.FINISHED)) View.VISIBLE else View.INVISIBLE
                binding.startGameBtn.setOnClickListener {
                    if (gameModel?.gameStatus == GameStatus.FINISHED) {
                        resetGame()
                    } else {
                        startGame()
                    }
                }

            }
        }
    }

    fun resetGame() {
        gameModel = GameModel(
            gameId = gameModel?.gameId ?: "",
            filledPos = MutableList(9) { "" },
            winner = "",
            gameStatus = GameStatus.CREATED,
            currentPlayer = if (Random.nextBoolean()) "X" else "O",
            player1Id = FirebaseAuth.getInstance().currentUser?.uid,
            player2Id = null,
            waitingForOpponent = false // Hoặc true nếu bạn muốn chờ người chơi khác tham gia
        )
        updateGameData(gameModel!!)
        gameModel = GameModel()
        setUI()
        startGame()
    }

    fun startGame(){
        gameModel?.apply {
            if (gameStatus in listOf(GameStatus.CREATED, GameStatus.JOINED)) {
                gameStatus = GameStatus.INPROGRESS
                updateGameData(this)
            } else {
                Toast.makeText(applicationContext, "Game has already started.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateGameData(model : GameModel){
        FirebaseDatabase.getInstance().getReference("games").child(model.gameId).setValue(model)
    }

    fun checkForWinner(){
        val winningPos = arrayOf(
            intArrayOf(0,1,2),
            intArrayOf(3,4,5),
            intArrayOf(6,7,8),
            intArrayOf(0,3,6),
            intArrayOf(1,4,7),
            intArrayOf(2,5,8),
            intArrayOf(0,4,8),
            intArrayOf(2,4,6),
        )

        gameModel?.apply {
            for (i in winningPos) {
                //012
                if (
                    filledPos[i[0]] == filledPos[i[1]] &&
                    filledPos[i[1]] == filledPos[i[2]] &&
                    filledPos[i[0]].isNotEmpty()
                ) {
                    gameStatus = GameStatus.FINISHED
                    winner = filledPos[i[0]]

                    if (gameStatus == GameStatus.FINISHED && winner != "Draw") {
                        updatePlayerScore(winner,true)
                    }
                    break
                }
            }

            if (gameStatus != GameStatus.FINISHED && filledPos.none { it.isEmpty() }) {
                // Nếu tất cả các ô đã được điền và không có người thắng
                gameStatus = GameStatus.FINISHED
                winner = "Draw" // Hoặc có thể để trống
            }



            updateGameData(this)
            if (gameStatus == GameStatus.FINISHED) {
                if (winner != "Draw") {
                    val winningPlayerId =
                        if (winner == "X") gameModel?.player1Id else gameModel?.player2Id
                    val losingPlayerId =
                        if (winner == "X") gameModel?.player2Id else gameModel?.player1Id

                    // Cập nhật điểm số cho người thắng và người thua
                    winningPlayerId?.let { updatePlayerScore(it, true) }
                    losingPlayerId?.let { updatePlayerScore(it, false) }
                }

                // Theo dõi và cập nhật điểm số
                fetchAndUpdateScores()
            }
        }

    }



    fun updatePlayerScore(playerId: String, isWinner: Boolean) {
        val playerScoreRef = FirebaseDatabase.getInstance().getReference("users/$playerId/score")
       playerScoreRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var score = mutableData.getValue(Int::class.java) ?: 0
                if (isWinner) {
                    score++ // Tăng điểm cho người thắng
                }
                mutableData.value = score
               return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot: DataSnapshot?) {
                Log.d("GameActivity", "updatePlayerScore:onComplete: $databaseError")
            }
        })
    }


    fun fetchAndUpdateScores() {
        val player1Id = FirebaseAuth.getInstance().currentUser?.uid
       val player2Id = gameModel?.player2Id.takeIf { it != player1Id } ?: gameModel?.player1Id

        listOf(player1Id to player2Id, player2Id to player1Id).forEach { (observerId, opponentId) ->
            FirebaseDatabase.getInstance().getReference("users/$observerId/score")
                .addValueEventListener(object : ValueEventListener {
                   override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val observerScore = dataSnapshot.getValue(Int::class.java) ?: 0
                        FirebaseDatabase.getInstance().getReference("users/$opponentId/score")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val opponentScore = dataSnapshot.getValue(Int::class.java) ?: 0
                                    updateScoreDisplay(observerScore, opponentScore)
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    // Xử lý lỗi
                               }
                            })
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Xử lý lỗi
                    }
                })
        }
    }



    fun updateScoreDisplay(playerScore: Int, opponentScore: Int) {
        binding.scoreText.text = "Score: $playerScore - $opponentScore"
    }

    fun resetScores() {
        // Đặt lại điểm số của cả hai người chơi về 0
        val player1Id = gameModel?.player1Id
        val player2Id = gameModel?.player2Id

        listOf(player1Id, player2Id).forEach { playerId ->
            playerId?.let {
                FirebaseDatabase.getInstance().getReference("users/$it/score")
                    .setValue(0)
            }
        }
    }

    override fun onClick(v: View?) {
        gameModel?.apply {
            if (gameStatus != GameStatus.INPROGRESS) {
                Toast.makeText(applicationContext, "Game not started", Toast.LENGTH_SHORT).show()
                return
            }

            val clickedPos = (v?.tag as String).toInt()
            if (filledPos[clickedPos].isEmpty()) {
                val currentUserIsX = FirebaseAuth.getInstance().currentUser?.uid == player1Id
                if ((currentPlayer == "X" && currentUserIsX) || (currentPlayer == "O" && !currentUserIsX)) {
                    filledPos[clickedPos] = currentPlayer
                    currentPlayer = if (currentPlayer == "X") "O" else "X"
                    checkForWinner()
                    updateGameData(this)

                } else {
                    Toast.makeText(applicationContext, "Not your turn", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun updateUsersCoin(userId: String, coin: Int) {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")

        // Lấy thông tin hiện tại của người dùng
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Giả sử bạn có trường 'coins' trong object của người dùng
                val currentCoins = dataSnapshot.child("coins").getValue(Double::class.java) ?: 0.0
                val newCoinValue = currentCoins + coin

                // Cập nhật số dư mới
                userRef.child("coins").setValue(newCoinValue)
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener {
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }
}

















