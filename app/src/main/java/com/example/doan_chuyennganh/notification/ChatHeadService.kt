package com.example.doan_chuyennganh.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ChatHeadService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

}
