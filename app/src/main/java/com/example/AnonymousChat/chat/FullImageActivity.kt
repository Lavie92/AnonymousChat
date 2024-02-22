package com.example.AnonymousChat.chat

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.AnonymousChat.R
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class FullImageActivity : AppCompatActivity() {

    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)

        val imageUrl = intent.getStringExtra("image_url")
        val imageView = findViewById<ImageView>(R.id.ivFullImage)
        val btnDownload = findViewById<Button>(R.id.btnDownload)

        Picasso.get().load(imageUrl).into(imageView)
        btnDownload.setOnClickListener {
            imageUrl?.let { url -> downloadImage(url) }
        }
    }

    private fun downloadImage(imageUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.connect()

                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)

                val filename = "${System.currentTimeMillis()}.jpg"
                var fos: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = contentValues()
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)

                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                        fos = contentResolver.openOutputStream(uri)
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                    val image = File(imagesDir, filename)
                    fos = FileOutputStream(image)
                }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FullImageActivity, "Ảnh đã được tải xuống", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FullImageActivity, "Lỗi khi tải ảnh: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun contentValues(): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
    }
}
