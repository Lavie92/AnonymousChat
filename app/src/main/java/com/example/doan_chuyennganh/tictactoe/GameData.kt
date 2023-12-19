package com.example.doan_chuyennganh.tictactoe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object GameData {
    private var _gameModel: MutableLiveData<GameModel> = MutableLiveData()
    var gameModel: LiveData<GameModel> = _gameModel

    fun saveGameModel(model: GameModel) {
        _gameModel.postValue(model)
        if (model.gameId.isNotEmpty()) {
            Firebase.firestore.collection("games")
                .document(model.gameId)
                .set(model)
        }
    }

    fun fetchGameModel(gameId: String) {
        Firebase.firestore.collection("games")
            .document(gameId)
            .addSnapshotListener { snapshot, error ->
                snapshot?.toObject(GameModel::class.java)?.let {
                    _gameModel.postValue(it)
                }
            }
    }

}
//package np.com.bimalkafle.tictactoeonline
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
//
//object GameData {
//    private var _gameModel : MutableLiveData<GameModel> = MutableLiveData()
//    var gameModel : LiveData<GameModel> = _gameModel
//    var myID = ""
//
//
//    fun saveGameModel(model : GameModel){
//        _gameModel.postValue(model)
//        if(model.gameId!="-1"){
//            Firebase.firestore.collection("games")
//                .document(model.gameId)
//                .set(model)
//        }
//
//    }
//
//    fun fetchGameModel(){
//        gameModel.value?.apply {
//            if(gameId!="-1"){
//                Firebase.firestore.collection("games")
//                    .document(gameId)
//                    .addSnapshotListener { value, error ->
//                        val model = value?.toObject(GameModel::class.java)
//                        _gameModel.postValue(model)
//                    }
//            }
//        }
//    }
//
//}
