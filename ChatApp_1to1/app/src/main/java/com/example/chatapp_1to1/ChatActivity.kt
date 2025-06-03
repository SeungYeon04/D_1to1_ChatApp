package com.example.chatapp_1to1

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var emojiButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var chatTitle: TextView
    private lateinit var statusText: TextView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomId: String
    private lateinit var currentUserUid: String
    
    // 커스텀 키보드 관련
    private lateinit var customKeyboardContainer: LinearLayout
    private var isCustomKeyboardVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Firebase 초기화
        firestore = FirebaseFirestore.getInstance()
        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        roomId = intent.getStringExtra("roomId") ?: ""

        if (roomId.isEmpty() || currentUserUid.isEmpty()) {
            Toast.makeText(this, "채팅방 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // View 초기화
        recyclerView = findViewById(R.id.recyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        emojiButton = findViewById(R.id.emojiButton)
        closeButton = findViewById(R.id.closeButton)
        chatTitle = findViewById(R.id.chatTitle)
        statusText = findViewById(R.id.statusText)
        customKeyboardContainer = findViewById(R.id.customKeyboardContainer)

        // 커스텀 키보드 설정
        setupCustomKeyboard()

        // 메인 레이아웃 클릭 시 키보드 숨기기
        findViewById<View>(R.id.main_layout)?.setOnClickListener {
            if (isCustomKeyboardVisible) {
                hideCustomKeyboard()
                messageInput.clearFocus()
            }
        }

        // 리사이클러뷰 클릭 시 키보드 숨기기
        recyclerView.setOnTouchListener { _, _ ->
            if (isCustomKeyboardVisible) {
                hideCustomKeyboard()
                messageInput.clearFocus()
            }
            false
        }

        // 헤더 영역 클릭 시 키보드 숨기기
        findViewById<View>(R.id.headerLayout)?.setOnClickListener {
            if (isCustomKeyboardVisible) {
                hideCustomKeyboard()
                messageInput.clearFocus()
            }
        }

        // 헤더 정보 설정
        setupHeader()

        // RecyclerView 설정
        chatAdapter = ChatAdapter(currentUserUid)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // 메시지 전송 버튼 클릭 리스너
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageInput.text.clear()
            }
        }

        // X 버튼 클릭 리스너
        closeButton.setOnClickListener {
            showExitConfirmDialog()
        }

        // 이모티콘 버튼 클릭 리스너
        emojiButton.setOnClickListener {
            Toast.makeText(this, "이모티콘 기능은 추후 구현 예정입니다.", Toast.LENGTH_SHORT).show()
        }

        // 실시간 메시지 수신 리스너 설정
        setupMessageListener()

        // 키 리스너 설정
        messageInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                val messageText = messageInput.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    sendMessage(messageText)
                    messageInput.text.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupHeader() {
        // 채팅방 제목은 Firestore에서 로드
        loadPartnerInfo()
        
        // 상태 텍스트 설정 (접속 상태 등으로 나중에 데이터 받아서 처리)
        val status = intent.getStringExtra("status") ?: "신남"
        statusText.text = status
        
        // 상태 텍스트 클릭 리스너 추가
        statusText.setOnClickListener {
            showStatusPopup(it)
        }
    }

    private fun loadPartnerInfo() {
        firestore.collection("rooms").document(roomId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val users = document.get("users") as? Map<String, Map<String, Any>>
                    users?.forEach { (uid, userInfo) ->
                        if (uid != currentUserUid) {
                            val partnerNickname = userInfo["nickname"] as? String ?: "알 수 없는 사용자"
                            chatTitle.text = partnerNickname
                            return@forEach
                        }
                    }
                } else {
                    chatTitle.text = "채팅상대"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "채팅방 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                chatTitle.text = "채팅상대"
            }
    }

    private fun showExitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("채팅방 나가기")
            .setMessage("정말 나가시겠습니까?")
            .setPositiveButton("나가기") { _, _ ->
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupMessageListener() {
        firestore.collection("rooms").document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "메시지를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val messages = mutableListOf<ChatMessage>()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(ChatMessage::class.java)?.let { messages.add(it) }
                }
                chatAdapter.updateMessages(messages)
                recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun sendMessage(text: String) {
        val message = ChatMessage(
            sender = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown",
            text = text,
            timestamp = System.currentTimeMillis(),
            senderUid = currentUserUid
        )

        firestore.collection("rooms").document(roomId)
            .collection("messages")
            .add(message)
            .addOnFailureListener { e ->
                Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showStatusPopup(anchorView: View) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.md_status_popup, null)
        
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // 팝업 바깥 영역 클릭 시 닫히도록 설정
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        
        // 팝업을 statusText 아래쪽에 표시
        popupWindow.showAsDropDown(anchorView, 0, 8)
    }

    private fun setupCustomKeyboard() {
        // 메시지 입력창 클릭 시에만 커스텀 키보드 표시
        messageInput.setOnClickListener {
            hideSystemKeyboard()
            showCustomKeyboard()
        }
        
        // 포커스 변경 시 자동으로 키보드가 나타나지 않도록 설정
        messageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideCustomKeyboard()
            }
        }
    }

    private fun hideSystemKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
    }

    private fun showCustomKeyboard() {
        if (!isCustomKeyboardVisible) {
            val inflater = LayoutInflater.from(this)
            val keyboardView = inflater.inflate(R.layout.custom_keyboard, null)
            
            customKeyboardContainer.removeAllViews()
            customKeyboardContainer.addView(keyboardView)
            customKeyboardContainer.visibility = View.VISIBLE
            isCustomKeyboardVisible = true
            
            // 입력창을 키보드 위로 이동
            adjustInputContainerPosition(true)
            
            setupKeyboardListeners(keyboardView)
        }
    }

    private fun hideCustomKeyboard() {
        customKeyboardContainer.visibility = View.GONE
        isCustomKeyboardVisible = false
        
        // 입력창을 화면 하단으로 이동
        adjustInputContainerPosition(false)
    }

    private fun adjustInputContainerPosition(keyboardVisible: Boolean) {
        val inputContainer = findViewById<LinearLayout>(R.id.inputContainer)
        val layoutParams = inputContainer.layoutParams as RelativeLayout.LayoutParams
        
        if (keyboardVisible) {
            // 키보드가 보일 때: 키보드 위에 위치
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            layoutParams.addRule(RelativeLayout.ABOVE, R.id.customKeyboardContainer)
        } else {
            // 키보드가 숨겨질 때: 화면 하단에 위치
            layoutParams.removeRule(RelativeLayout.ABOVE)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        }
        
        inputContainer.layoutParams = layoutParams
    }

    private fun setupKeyboardListeners(keyboardView: View) {
        // 한글 자음/모음 처리 (기본적인 예시)
        val keys = arrayOf("ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ",
                          "ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ",
                          "ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ")
        
        val keyIds = arrayOf(R.id.key_ㅂ, R.id.key_ㅈ, R.id.key_ㄷ, R.id.key_ㄱ, R.id.key_ㅅ, 
                            R.id.key_ㅛ, R.id.key_ㅕ, R.id.key_ㅑ, R.id.key_ㅐ, R.id.key_ㅔ,
                            R.id.key_ㅁ, R.id.key_ㄴ, R.id.key_ㅇ, R.id.key_ㄹ, R.id.key_ㅎ,
                            R.id.key_ㅗ, R.id.key_ㅓ, R.id.key_ㅏ, R.id.key_ㅣ,
                            R.id.key_ㅋ, R.id.key_ㅌ, R.id.key_ㅊ, R.id.key_ㅍ, R.id.key_ㅠ, 
                            R.id.key_ㅜ, R.id.key_ㅡ)

        // 한글 키 설정
        for (i in keys.indices) {
            keyboardView.findViewById<Button>(keyIds[i])?.setOnClickListener {
                appendToInput(keys[i])
            }
        }

        // 숫자 키 설정
        for (i in 0..9) {
            val keyId = resources.getIdentifier("key_$i", "id", packageName)
            keyboardView.findViewById<Button>(keyId)?.setOnClickListener {
                appendToInput(i.toString())
            }
        }

        // 특수 키 설정
        keyboardView.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            appendToInput(" ")
        }

        keyboardView.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            deleteLastCharacter()
        }

        keyboardView.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageInput.text.clear()
            }
        }
    }

    private fun appendToInput(text: String) {
        val currentText = messageInput.text.toString()
        val cursorPosition = messageInput.selectionStart
        val newText = currentText.substring(0, cursorPosition) + text + currentText.substring(cursorPosition)
        messageInput.setText(newText)
        messageInput.setSelection(cursorPosition + text.length)
    }

    private fun deleteLastCharacter() {
        val currentText = messageInput.text.toString()
        val cursorPosition = messageInput.selectionStart
        if (cursorPosition > 0) {
            val newText = currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition)
            messageInput.setText(newText)
            messageInput.setSelection(cursorPosition - 1)
        }
    }

    override fun onBackPressed() {
        if (isCustomKeyboardVisible) {
            hideCustomKeyboard()
        } else {
            super.onBackPressed()
        }
    }
} 