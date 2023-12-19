package com.example.doan_chuyennganh.tictactoe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.doan_chuyennganh.databinding.ActivityTictactoeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random
import kotlin.random.nextInt

class TictactoeActivity : AppCompatActivity() {
    lateinit var binding: ActivityTictactoeBinding
    private val database = FirebaseDatabase.getInstance().getReference("games")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTictactoeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playOfflineBtn.setOnClickListener {
            createOfflineGame()
        }

        binding.createOnlineGameBtn.setOnClickListener {
            createOnlineGame()
        }

        binding.joinOnlineGameBtn.setOnClickListener {
            joinOrCreateGame()
        }

    }
    private fun joinOrCreateGame() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            database.orderByChild("waitingForOpponent").equalTo(true).limitToFirst(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val game = snapshot.children.first()
                            joinGame(game.key!!, uid)
                        } else {
                            createNewGame(uid)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@TictactoeActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
    private fun createNewGame(userId: String) {
        val newGameId = database.push().key!!
        val newGame = GameModel(gameId = newGameId, player1Id = userId)
        database.child(newGameId).setValue(newGame)
        navigateToGameScreen(newGameId)
    }

    private fun joinGame(gameId: String, userId: String) {
        database.child(gameId).updateChildren(mapOf("player2Id" to userId, "waitingForOpponent" to false))
        navigateToGameScreen(gameId)
    }
    private fun navigateToGameScreen(gameId: String) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("GAME_ID", gameId)
        }
        startActivity(intent)
    }
    fun createOfflineGame(){
        GameData.saveGameModel(
            GameModel(
                gameStatus = GameStatus.JOINED
            )
        )
        startGame()
    }

    fun createOnlineGame(){
        GameData.myID = "X"
        GameData.saveGameModel(
            GameModel(
                gameStatus = GameStatus.CREATED,
                gameId = Random.nextInt(1000..9999).toString()
            )
        )
        startGame()
    }

    fun joinOnlineGame(){
        var gameId = binding.gameIdInput.text.toString()
        if(gameId.isEmpty()){
            binding.gameIdInput.setError("Please enter game ID")
            return
        }
        GameData.myID = "O"
        Firebase.firestore.collection("games")
            .document(gameId)
            .get()
            .addOnSuccessListener {
                val model = it?.toObject(GameModel::class.java)
                if(model==null){
                    binding.gameIdInput.setError("Please enter valid game ID")
                }else{
                    model.gameStatus = GameStatus.JOINED
                    GameData.saveGameModel(model)
                    startGame()
                }
            }

    }

    fun startGame(){
        startActivity(Intent(this,GameActivity::class.java))
    }

}