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
//package np.com.bimalkafle.tictactoeonline
//
//import android.content.Intent
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.widget.Toast
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.firestore.ktx.toObject
//import com.google.firebase.ktx.Firebase
//import np.com.bimalkafle.tictactoeonline.databinding.ActivityMainBinding
//import kotlin.random.Random
//import kotlin.random.nextInt
//
//class MainActivity : AppCompatActivity() {
//
//    lateinit var binding : ActivityMainBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.playOfflineBtn.setOnClickListener {
//            createOfflineGame()
//        }
//
//        binding.createOnlineGameBtn.setOnClickListener {
//            createOnlineGame()
//        }
//
//        binding.joinOnlineGameBtn.setOnClickListener {
//            joinOnlineGame()
//        }
//
//    }
//
//
//    fun createOfflineGame(){
//        GameData.saveGameModel(
//            GameModel(
//                gameStatus = GameStatus.JOINED
//            )
//        )
//        startGame()
//    }
//
//    fun createOnlineGame(){
//        GameData.myID = "X"
//        GameData.saveGameModel(
//            GameModel(
//                gameStatus = GameStatus.CREATED,
//                gameId = Random.nextInt(1000..9999).toString()
//            )
//        )
//        startGame()
//    }
//
//    fun joinOnlineGame(){
//        var gameId = binding.gameIdInput.text.toString()
//        if(gameId.isEmpty()){
//            binding.gameIdInput.setError("Please enter game ID")
//            return
//        }
//        GameData.myID = "O"
//        Firebase.firestore.collection("games")
//            .document(gameId)
//            .get()
//            .addOnSuccessListener {
//                val model = it?.toObject(GameModel::class.java)
//                if(model==null){
//                    binding.gameIdInput.setError("Please enter valid game ID")
//                }else{
//                    model.gameStatus = GameStatus.JOINED
//                    GameData.saveGameModel(model)
//                    startGame()
//                }
//            }
//
//    }
//
//    fun startGame(){
//        startActivity(Intent(this,GameActivity::class.java))
//    }
//
//}