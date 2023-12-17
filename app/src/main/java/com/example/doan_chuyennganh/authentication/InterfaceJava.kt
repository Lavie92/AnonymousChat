package com.example.doan_chuyennganh.authentication

import android.webkit.JavascriptInterface
import com.example.doan_chuyennganh.voice_chat.CallActivity

class InterfaceJava(var callActivity: CallActivity?) {

    @JavascriptInterface
    fun onPeerConnected() {
        callActivity?.onPeerConnected()
    }
}
