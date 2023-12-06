//package com.example.doan_chuyennganh
//
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.view.View
//import android.widget.EditText
//import android.widget.ImageButton
//import android.widget.TextView
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.doan_chuyennganh.chat.MessageAdapter
//import com.example.doan_chuyennganh.databinding.ActivityChatbotBinding
//import com.example.doan_chuyennganh.databinding.ActivityMainBinding
//import com.google.common.net.MediaType
//import com.malkinfo.chatgpts.Message
//import com.malkinfo.chatgpts.MessageAdapter
//import okhttp3.*
//import okhttp3.MediaType.Companion.toMediaType
//import org.json.JSONArray
//import org.json.JSONException
//import org.json.JSONObject
//import java.io.IOException
//import java.util.concurrent.TimeUnit
//
//class ChatbotActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityChatbotBinding
//    val API_KEY ="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"
//    lateinit var recyclerView: RecyclerView
//    lateinit var welcomeText :TextView
//    lateinit var messageEditText:EditText
//    lateinit var sendButton:ImageButton
//    lateinit var messageList:MutableList<Message>
//    lateinit var messageAdapter: MessageAdapter
//    val client =  OkHttpClient.Builder().readTimeout(120, TimeUnit.SECONDS).build();
//
//
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        binding = ActivityChatbotBinding.inflate(layoutInflater)
//        super.onCreate(savedInstanceState)
//        setContentView(binding.root)
//        messageList = ArrayList()
//        recyclerView = binding.recyclerView
//        welcomeText = binding.welcomeText
//        messageEditText = binding.messageEditText
//        sendButton = binding.sendBt
//        messageAdapter = MessageAdapter(messageList)
//        recyclerView.adapter = messageAdapter
//        val layoutManger = LinearLayoutManager(this)
//        layoutManger.stackFromEnd = true
//        recyclerView.layoutManager = layoutManger
//
//        sendButton.setOnClickListener {
//            val question = messageEditText.text.toString().trim{ it <= ' '}
//            addToChat(question, Message.SENT_BY_ME)
//            messageEditText.setText("")
//            callAPI(question)
//            welcomeText.visibility = View.GONE
//        }
//    }
//
//    private fun addToChat(message: String, sentBy: String) {
//        runOnUiThread{
//            messageList.add(Message(message,sentBy))
//            messageAdapter.notifyDataSetChanged()
//            recyclerView.smoothScrollToPosition(messageAdapter.itemCount)
//        }
//
//    }
//
//    fun addResponse(response:String?){
//        messageList.removeAt(messageList.size -1)
//        addToChat(response!!, Message.SENT_BY_BOT)
//
//    }
//
//    private fun callAPI(question: String) {
//        //call okhttp
//        messageList.add(Message("Typing...", Message.SENT_BY_BOT))
//        val jsonBody = JSONObject()
//        val messageArr = JSONArray()
//        try {
//            jsonBody.put( "model",  "gpt-3.5-turbo");
//            val userMessage = JSONObject()
//            userMessage.put("role", "user")
//            userMessage.put("content", question)
//            messageArr.put(userMessage)
//            jsonBody.put("messages", messageArr)
//
//        }catch (e:JSONException){
//            e.printStackTrace()
//        }
//        val body :RequestBody = RequestBody.create(JSON,jsonBody.toString())
//        val request:Request = Request.Builder()
//            .url("https://api.openai.com/v1/chat/completions")
//            .header("Authorization", "Bearer $API_KEY")
//            .post(body)
//            .build()
//        client.newCall(request).enqueue(object :Callback{
//            override fun onFailure(call: Call, e: IOException) {
//                addResponse("Failed to load response due to ${e.message}")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                if (response.isSuccessful){
//                    var jsonObject :JSONObject? = null
//                    try {
//                        jsonObject = JSONObject(response.body!!.string())
//                        val jsonArray = jsonObject.getJSONArray("choices")
//                        val result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
//                        addResponse(result.trim{it <= ' '})
//                    }catch (e:JSONException){
//                        e.printStackTrace()
//                    }
//                }else{
//                    addResponse("Failed to load response due to ${response.body?.string()}")
//                }
//            }
//
//        })
//
//    }
//    companion object{
//        val JSON : MediaType = "application/json; charset=utf-8".toMediaType()
//    }
//}