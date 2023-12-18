package com.example.doan_chuyennganh.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.example.doan_chuyennganh.R
import com.squareup.picasso.Picasso

class FullImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)
        val imageUrl = intent.getStringExtra("image_url")
        val imageView = findViewById<ImageView>(R.id.ivFullImage)
        Picasso.get().load(imageUrl).into(imageView)
    }
}