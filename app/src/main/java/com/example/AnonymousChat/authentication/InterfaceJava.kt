package com.example.AnonymousChat.authentication

import android.webkit.JavascriptInterface
import com.example.AnonymousChat.voice_chat.CallActivity

class InterfaceJava(private var callActivity: CallActivity?) {

    @JavascriptInterface
    fun onPeerConnected() {
        callActivity?.onPeerConnected()
    }
}
