package com.example.doan_chuyennganh.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.doan_chuyennganh.encrypt.EncryptionUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID
private const val STORAGE_PATH = "images/"

class ImageUtils(private val activity: Activity) {

    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    fun showImagePickerDialog() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        activity.startActivityForResult(
            Intent.createChooser(intent, "Chọn ảnh"),
            ChatActivity.REQUEST_CODE
        )
    }

    fun uploadImageToFirebaseStorage(imageUri: Uri, chatRoomId: String, chatType: String, currentUserId: String, receiverId: String) {
        checkAndDeleteOldImages()
        val imageName = UUID.randomUUID().toString()
        val imageRef = storageRef.child("$STORAGE_PATH$imageName.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    handleImageUploadSuccess(imageUrl, chatRoomId, chatType, currentUserId, receiverId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, "Lỗi khi tải ảnh lên: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun handleImageUploadSuccess(imageUrl: String, chatRoomId: String, chatType: String, currentUserId: String, receiverId: String) {
        val timestamp = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        val secretKey = EncryptionUtils.generateKey()
        val encryptedMessage = EncryptionUtils.encrypt(imageUrl, secretKey)
        val encryptedKey = EncryptionUtils.getKeyAsString(secretKey)
        val type = "image"
        val message =
            Message(messageId, currentUserId, receiverId, encryptedMessage, type, encryptedKey, timestamp)

        FirebaseDatabase.getInstance().getReference(chatType).child(chatRoomId)
            .child("messages").push().setValue(message)

        Toast.makeText(activity, "Ảnh đã được gửi", Toast.LENGTH_SHORT).show()
        updateUserCoin(currentUserId)
    }

    private fun updateUserCoin(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentCoins = dataSnapshot.child("coins").getValue(Double::class.java) ?: 0.0
                val coin = 3;
                val newCoinValue = currentCoins - coin
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
    private fun checkAndDeleteOldImages() {
        val oldImageThreshold = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val oldImagesQuery = storageRef.child(STORAGE_PATH).listAll()

        oldImagesQuery.addOnSuccessListener { result ->
            for (fileRef in result.items) {
                // Lấy metadata của file để kiểm tra thời gian tải lên
                fileRef.metadata.addOnSuccessListener { metadata ->
                    val uploadedTime = metadata.creationTimeMillis

                    // Nếu thời gian tải lên của hình cũ hơn ngưỡng cho phép, thì xoá nó
                    if (uploadedTime < oldImageThreshold) {
                        fileRef.delete().addOnSuccessListener {
                            // Xoá thành công, bạn có thể thực hiện các hành động khác nếu cần
                        }.addOnFailureListener { e ->
                            // Xoá thất bại, xử lý lỗi nếu cần
                        }
                    }
                }
            }
        }
    }
}
