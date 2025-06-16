package com.example.chatapp_1to1

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.view.View.OnClickListener

class ChatActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var emojiButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var chatTitle: TextView
    private lateinit var emotionBadgeLayout: LinearLayout
    private lateinit var emotionCircle: View
    private lateinit var emotionText: TextView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomId: String
    private lateinit var currentUserUid: String
    
    // 감정 상태를 나타내는 enum class 수정
    private enum class EmotionState(
        val text: String, 
        val backgroundColor: Int,
        val textColor: Int
    ) {
        HAPPY(
            "신남", 
            Color.parseColor("#87CEEB"),  // 하늘색 배경
            Color.parseColor("#000080")   // 진한 파란색 글자
        ),
        NEUTRAL(
            "보통", 
            Color.parseColor("#D3D3D3"),  // 회색 배경
            Color.parseColor("#000000")   // 검정색 글자
        ),
        SAD(
            "우울", 
            Color.parseColor("#00008B"),  // 짙은 파란색 배경
            Color.parseColor("#FFFFFF")   // 흰색 글자
        ),
        ANGRY(
            "화남", 
            Color.parseColor("#FF0000"),  // 빨간색 배경
            Color.parseColor("#FFFFFF")   // 흰색 글자
        )
    }

    // 감정 점수를 저장할 변수
    private var emotionScore = 0

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
        emotionBadgeLayout = findViewById(R.id.emotionBadge)
        emotionCircle = findViewById(R.id.emotionCircle)
        emotionText = findViewById(R.id.emotionText)

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
        
        // 초기 상태 설정
        updateEmotionStatus(0)
        
        // 상태 텍스트 클릭 리스너 추가
        emotionBadgeLayout.setOnClickListener {
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
                    doc.toObject(ChatMessage::class.java)?.let { message ->
                        messages.add(message)
                        // 상대방의 메시지인 경우에만 감정 분석 수행
                        if (message.senderUid != currentUserUid) {
                            analyzeEmotion(message.text)
                        }
                    }
                }
                chatAdapter.updateMessages(messages)
                recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun analyzeEmotion(text: String) {
        // 긍정적인 단어들
        val positiveWords = listOf("좋아", "행복", "즐거워", "웃겨", "재밌어", "최고", "사랑해", "감사", "고마워", "응원해", "하하", "ㅋㅋ", "ㅎㅎ")
        // 부정적인 단어들
        val negativeWords = listOf("싫어", "화나", "짜증", "힘들어", "슬퍼", "우울", "미워", "실망", "불만", "화나", "ㅠㅠ", "ㅜㅜ", "흑")

        // 텍스트에서 감정 점수 계산
        var score = 0
        positiveWords.forEach { word ->
            if (text.contains(word)) score += 2  // 점수 가중치 증가
        }
        negativeWords.forEach { word ->
            if (text.contains(word)) score -= 2  // 점수 가중치 증가
        }

        // 감정 점수 업데이트 (최근 메시지의 영향이 더 크도록 가중치 부여)
        emotionScore = (emotionScore * 0.3 + score * 0.7).toInt()  // 가중치 조정
        
        // 감정 상태에 따라 UI 업데이트
        updateEmotionStatus(emotionScore)
    }

    private fun updateEmotionStatus(score: Int) {
        val emotionState = when {
            score >= 3 -> EmotionState.HAPPY
            score >= 0 -> EmotionState.NEUTRAL
            score >= -3 -> EmotionState.SAD
            else -> EmotionState.ANGRY
        }

        // UI 업데이트는 메인 스레드에서 실행
        runOnUiThread {
            // 감정 텍스트 설정
            emotionText.text = emotionState.text
            emotionText.setTextColor(emotionState.backgroundColor)
            // 감정 원형 색상 설정
            val circleDrawable = emotionCircle.background as GradientDrawable
            circleDrawable.setColor(emotionState.backgroundColor)
            // 감정 텍스트 스타일
            emotionText.textSize = 16f
            emotionText.setTypeface(null, android.graphics.Typeface.BOLD)
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