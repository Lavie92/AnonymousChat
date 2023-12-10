package com.example.doan_chuyennganh

import android.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doan_chuyennganh.active.SharedPreferencesManager
import com.example.doan_chuyennganh.authentication.NotificationActivity
import com.example.doan_chuyennganh.authentication.SettingActivity
import com.example.doan_chuyennganh.databinding.ActivityProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class ProfileActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityProfileBinding
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var isActive: Boolean = false
    private val calendar = Calendar.getInstance()
    private var isDataChanged = false
    private var isDOBEnabled = true
    private var isDOBChanged = false
    private lateinit var sharedPreferencesManager: SharedPreferencesManager


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        databaseReferences = FirebaseDatabase.getInstance().getReference("users")

        sharedPreferencesManager = SharedPreferencesManager(this)
        checkSession()
        binding.btnBack.setOnClickListener{
            onBackPressed()
        }

        binding.tvPoint.setOnTouchListener{view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.tvPoint.tooltipText = "Với mỗi lần VI PHẠM, bạn sẽ bị trừ 10 điểm. Nếu điểm của bạn về 0, bạn sẽ bị CHẶN trên hệ thống!"

                true // Consume the touch event
            } else if (event.action == MotionEvent.ACTION_UP) {

            }
            false // Don't consume other touch events (optional)
        }


        val userId = auth.currentUser?.uid
        val currentUser = auth.currentUser
        if (userId != null) {
            databaseReferences.child(userId).get().addOnSuccessListener {
                if (it.exists()) {
                    isActive = it.child("active").value as Boolean
                }
            }
        }else{
            Toast.makeText(this,"No user signed in!",Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
        }
        //Read data
        readData(userId) { dataLoaded ->
            if (dataLoaded) {
                // Dữ liệu đã được load, kiểm tra nếu có thay đổi
                checkDataChanges()
                checkAge()
                checkButtonAvailability(userId)
            } else {
                // Có lỗi khi load dữ liệu
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        }

        //
        binding.btnSetting.setOnClickListener{
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.btnNoti.setOnClickListener{
            startActivity(Intent(this, NotificationActivity::class.java))

        }

            binding.btnSave.setOnClickListener{
                val username = binding.txtChangeName.text.toString()
                val age = binding.txtChangeAge.text.toString()
                val gender = binding.genderSpinner.selectedItem.toString()
                if (userId != null) {
                    if (currentUser != null) {
                        updateData(currentUser,username,age,gender)
                    }
                } else {
                    Toast.makeText(this,"No user signed in!",Toast.LENGTH_SHORT).show()
                }

            }

    }

    //end Session check
    //Check Function
    private fun checkAge(){
        val check = binding.txtViewage.text.toString()
        if(!check.isNullOrBlank()){
            if(check.toInt() < 16){
                binding.switchFilter.isClickable = false
            }
        }

    }
    private fun checkDataChanges()
    {
        // Sự kiện khi nội dung của EditText thay đổi
        binding.txtChangeName.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            isDataChanged = true
        }

        override fun afterTextChanged(s: Editable?) {
        }
    })


    binding.genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            isDataChanged = true
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }
        checkAgeChanges()
        checkSwitchFilterChanges()

    }
    private fun checkSwitchFilterChanges() {
        var previousSwitchState = binding.switchFilter.isChecked
        binding.switchFilter.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchFilter.isChecked != previousSwitchState) {
                isDataChanged = true
            }
        }
    }

    private fun checkAgeChanges() {
        binding.txtViewage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isDataChanged = true
                isDOBChanged = true

            }
            override fun afterTextChanged(s: Editable?) {
            }
        })
    }
    private suspend fun checkUsernameExistence(username: String): Boolean {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        return try {
            val dataSnapshot = databaseReference.orderByChild("username").equalTo(username).get().await()
            dataSnapshot.exists()
        } catch (e: Exception) {

            false
        }
    }


    private fun checkValue() :Boolean{
        val username = binding.txtChangeName.text.toString()
        val age = binding.txtChangeAge.text.toString()

        if (username.isNullOrBlank() || username.length < 3) {
            showError(binding.txtChangeName, "Username must be at least 3 characters")
            return false
        } else if (age.isNullOrBlank()) {
            binding.txtChangeAge.error = "Age is not valid!"
            return false
        } else {
            val usernameExists = runBlocking { checkUsernameExistence(username) }

            return if (usernameExists) {
                showError(binding.txtChangeName, "Username is already taken")
                false
            } else {
                true
            }
        }
    }

    //End Check Functions

    //onFunctions
    override fun onResume() {
        super.onResume()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Optional: Finish the current activity to prevent going back to it
        }
    }

    override fun onBackPressed() {
        if(!isActive){
            Toast.makeText(this, "Please fill Your information!", Toast.LENGTH_SHORT).show()
        }
        else if (isDataChanged) {
            showConfirmationDialog()
        }
    else {
            super.onBackPressed()
        }
    }

    //End onFunctions

    //Dialogs

    private fun showSetAge() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmation")
        builder.setMessage("Bạn sẽ không thể thay đổi ngày sinh trong vòng 3 ngày tới, Bạn có chắc chắn với ngày sinh hiện tại?")

        builder.setPositiveButton("Có") { _: DialogInterface, _: Int ->

        }

        builder.setNegativeButton("Không") { _: DialogInterface, _: Int ->
            recreate()

        }

        builder.show()
    }

    private fun showError(input :EditText, err: String){
        input.error = err
        input.requestFocus()
    }

    private fun showErrorDialog() {
        // Lấy userId của người dùng hiện tại
        val userId = auth.currentUser?.uid

        // Lấy thời gian chờ còn lại dựa trên userId
        val remainingTime = calculateRemainingTime(userId)

        // Hiển thị dialog với thông báo thời gian chờ còn lại
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")

        val remainingDAYS = remainingTime / (24*60*60*1000)
        val message = "Bạn cần chờ ít nhất $remainingDAYS ngày trước khi ấn lại nút này."
        builder.setMessage(message)

        builder.setPositiveButton("OK") { _, _ ->
            // Đóng dialog khi người dùng ấn OK
        }
        builder.show()
    }
    //End Confirm Dialog


    //Database
    private fun updateData(user: FirebaseUser?, username: String, age: String, gender: String) {
        // Cập nhật thông tin người dùng
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        user?.updateProfile(profileUpdates)

        // Cập nhật thông tin trong Firebase Realtime Database
        val userId = user?.uid
        val userUpdate = mapOf(
            "username" to username,
            "age" to age,
            "gender" to gender,
            "active" to true,
            "filter" to binding.switchFilter.isChecked
        )

        if (userId != null) {
            if(checkValue()){
                databaseReferences.child(userId).updateChildren(userUpdate)
                    .addOnSuccessListener {
                        isActive = true
                        isDataChanged = false
                        Toast.makeText(this, "User data updated successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))

                        val check = binding.txtViewage.text.toString()


                        if(isDOBChanged == true){
                            binding.txtChangeAge.isClickable = false
                            val currentTime = Calendar.getInstance().timeInMillis
                            sharedPreferencesManager.saveLastClickTime(userId, currentTime)
                            Handler().postDelayed({
                                binding.txtChangeAge.isClickable = true
                                isDOBEnabled = true
                            }, TimeUnit.DAYS.toMillis(3))//TimeUnit.DAYS.toMillis(3)
                            if(check.toInt() < 16){
                                binding.switchFilter.isClickable = false
                                isDOBEnabled = false
                            }else{
                                binding.switchFilter.isClickable = true
                                isDOBEnabled = true
                            }
                        }


                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun readData(id: String?, callback: (Boolean) -> Unit) {
        val spinner = binding.genderSpinner
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        if (id != null) {
            databaseReferences.child(id).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Lấy giá trị từ snapshot
                    val username = snapshot.child("username").value
                    val email = snapshot.child("email").value
                    val gender = snapshot.child("gender").value
                    val active = snapshot.child("active").value
                    val age = snapshot.child("age").value
                    val filter = snapshot.child("filter").value
                    val chatRoom = snapshot.child("chatRoom").value
                    val point = snapshot.child("point").value

                    binding.imgProgess.progress= point.toString().toInt()
                    binding.tvPoint.text = point.toString()


                            // Binding giá trị vào các thành phần UI
                    binding.txtChangeName.setText(username.toString())
                    binding.txtChangeEmail.setText(email.toString())
                    val position = getIndexFromValue(spinner, gender.toString())
                    spinner.setSelection(position)
                    binding.name.text = "@$username"
                    binding.switchFilter.isChecked = filter as Boolean
                    if(age.toString() != ""){
                        binding.txtChangeAge.text = age.toString()
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val formattedDateOfBirth = age.toString()
                        val dateOfBirth = dateFormat.parse(formattedDateOfBirth)
                        // Hiển thị tuổi
                        try{


                            // Tính toán tuổi từ ngày sinh
                            val currentDate = Calendar.getInstance()
                            val dateOfBirthCalendar = Calendar.getInstance()
                            dateOfBirthCalendar.time = dateOfBirth

                            val age = currentDate.get(Calendar.YEAR) - dateOfBirthCalendar.get(Calendar.YEAR)
                            val isBirthdayPassed = currentDate.get(Calendar.DAY_OF_YEAR) >= dateOfBirthCalendar.get(Calendar.DAY_OF_YEAR)
                            val adjustedAge = if (isBirthdayPassed) age else age - 1

                            // Hiển thị tuổi
                            binding.txtViewage.text = adjustedAge.toString()

                        }catch (e: ParseException){
                            e.printStackTrace()
                        }

                    }

                    try {

                        // Gọi callback với thông báo rằng dữ liệu đã được load thành công
                        callback(true)
                    } catch (e: ParseException) {
                        // Xử lý lỗi nếu ngày không đúng định dạng
                        e.printStackTrace()

                        // Gọi callback với thông báo rằng có lỗi khi load dữ liệu
                        callback(false)
                    }
                } else {
                    // Gọi callback với thông báo rằng không có dữ liệu
                    callback(false)
                }
            }.addOnFailureListener {
                // Xử lý lỗi khi đọc dữ liệu
                // Gọi callback với thông báo rằng có lỗi khi load dữ liệu
                callback(false)
            }
        } else {
            // Gọi callback với thông báo rằng có lỗi khi load dữ liệu (user là null)
            callback(false)
        }
    }
    private fun getIndexFromValue(spinner: Spinner, value: String?): Int {
        val adapter = spinner.adapter as ArrayAdapter<String>
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == value) {
                return i
            }
        }
        return 0
    }
    //End database






    //Datetime Picker

    fun showDateTimePicker(view: View) {
        if(isDOBEnabled == true){
            showDatePicker()

        }else{
            showErrorDialog()
        }
        //checkButtonAvailability()

    }

    private fun showDatePicker() {

        // Parse the date from the EditText
        val dateStr = binding.txtChangeAge.text.toString()

        // Check if dateStr is not null and not blank
        if (dateStr.isNotBlank()) {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = dateFormat.parse(dateStr)

            // Check if date is not null
            if (date != null) {
                calendar.time = date
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                updateSelectedDateTime()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()


    }



    private fun updateSelectedDateTime() {
        // Format ngày sinh
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val formattedDateOfBirth = dateFormat.format(calendar.time)
        val currentDate = Calendar.getInstance()
        val dateOfBirthCalendar = Calendar.getInstance()

        dateOfBirthCalendar.time = calendar.time

        val age = currentDate.get(Calendar.YEAR) - dateOfBirthCalendar.get(Calendar.YEAR)

        // Kiểm tra xem ngày sinh đã qua hay chưa trong năm nay
        val isBirthdayPassed = currentDate.get(Calendar.DAY_OF_YEAR) >= dateOfBirthCalendar.get(Calendar.DAY_OF_YEAR)

        // Nếu chưa qua sinh nhật, giảm đi 1
        val adjustedAge = if (isBirthdayPassed) age else age - 1

        showSetAge()
        binding.txtChangeAge.text = formattedDateOfBirth
        binding.txtViewage.text = adjustedAge.toString()
    }

    private fun checkButtonAvailability(userId: String?) {
        val lastClickTime = userId?.let { sharedPreferencesManager.getLastClickTime(it) } ?: 0
        val currentTime = Calendar.getInstance().timeInMillis

        // Kiểm tra nếu đã đủ 3 ngày từ lần cuối cùng nút được ấn
        if (currentTime - lastClickTime >= TimeUnit.DAYS.toMillis(3)) {
            binding.txtChangeAge.isClickable = true
            isDOBEnabled = true
        } else {
            binding.txtChangeAge.isClickable = false
            isDOBEnabled = false
        }
    }
    private fun calculateRemainingTime(userId: String?): Long {
        // Lấy thời gian click cuối cùng cho người dùng hiện tại
        val lastClickTime = userId?.let { sharedPreferencesManager.getLastClickTime(it) } ?: 0

        // Lấy thời gian hiện tại
        val currentTime = Calendar.getInstance().timeInMillis

        // Tính thời gian còn lại (đổi từ millisecond sang ngày)
        val elapsedTime = currentTime - lastClickTime
        val remainingDAYS = (TimeUnit.DAYS.toMillis(3) - elapsedTime) //(5000 - elapsedTime)
        return if (remainingDAYS > 0) remainingDAYS else 0
    }
    companion object {
        const val PREF_NAME = "ButtonPref"
        const val LAST_CLICK_TIME = "LastClickTime"
    }
    //end Datetime Picker

    //Sessions check
    private fun getSessionId(): String? {
        val sharedPref = getSharedPreferences("PreSession2", Context.MODE_PRIVATE)
        return sharedPref.getString("sessionID2", null)
    }
    private fun checkSession() {
        val sessionId = getSessionId()
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        val user = auth.currentUser
        user?.let {
            databaseReferences.child(it.uid).child("session")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentSessionID = snapshot.value as String?
                        if(sessionId != currentSessionID){
                            showConfirmationDialog()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }
    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Thông báo!")
        builder.setMessage("Tài khoản này đang được đăng nhập ở thiết bị khác, vui lòng đăng nhập lại!")

        builder.setPositiveButton("OK") { _: DialogInterface, _: Int ->
            signOutAndStartSignInActivity()
            handleLogout()
            finish()
        }


        builder.show()
    }
    private fun signOutAndStartSignInActivity() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))


    }
    private fun handleLogout() {
        // Tạo Intent để chuyển hướng đến LoginActivity và xóa toàn bộ Activity đã mở trước đó
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Kết thúc Activity hiện tại
        finish()
    }


}