package com.example.doan_chuyennganh.tictactoe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.doan_chuyennganh.databinding.ActivityTictactoeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TictactoeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTictactoeBinding
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val currentUserID: String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTictactoeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.playOfflineBtn.setOnClickListener {
            startGameOffline()
        }
        binding.joinOnlineGameBtn.setOnClickListener {
            findOpponentAndStartGame()
        }
    }
    private fun startGameOffline() {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("OFFLINE_GAME", true)
        }
        startActivity(intent)
    }
    private fun findOpponentAndStartGame() {
        currentUserID?.let { userId ->
            // Tìm kiếm trò chơi chưa hoàn thành
            firebaseDatabase.getReference("games")
                .orderByChild("waitingForOpponent")
                .equalTo(true)
                .limitToFirst(1)
                .get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.childrenCount > 0) {
                        // Có trò chơi đang chờ, tham gia vào trò chơi đó
                        val game = dataSnapshot.children.first()
                        val gameId = game.key!!
                        updateGameSessionWithSecondPlayer(gameId, userId)
                    } else {
                        // Không có trò chơi nào, tạo trò chơi mới
                        createNewGame(userId)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Unable to find or create a game.", Toast.LENGTH_SHORT).show()
                }
        }?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNewGame(userId: String) {
        val gameId = firebaseDatabase.getReference("games").push().key!!
        firebaseDatabase.getReference("games/$gameId")
            .setValue(GameModel(gameId = gameId, player1Id = userId, waitingForOpponent = true))
        navigateToGameScreen(gameId)
    }

    private fun updateGameSessionWithSecondPlayer(gameId: String, userId: String) {
        firebaseDatabase.getReference("games/$gameId")
            .updateChildren(mapOf("player2Id" to userId, "waitingForOpponent" to false))
        navigateToGameScreen(gameId)
    }

    private fun navigateToGameScreen(gameId: String) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("GAME_ID", gameId)
        }
        startActivity(intent)
    }
}
