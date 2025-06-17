package com.example.chatapp_1to1

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
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

        // 헤더 정보 설정
        setupHeader()

        // 메시지 입력창 설정
        setupMessageInput()

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
                var Intent = Intent(applicationContext, PlantCareActivity::class.java)
                startActivity(Intent)
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

    private fun setupMessageInput() {
        // 입력창 클릭 시 포커스만 주기
        messageInput.setOnClickListener {
            messageInput.requestFocus()
        }
        
        // 엔터키로 메시지 전송
        messageInput.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
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
} 