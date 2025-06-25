package com.example.chatapp_1to1

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
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
                if (messageText.contains("사랑해")) {
                    completeQuest()
                }
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

    private fun completeQuest() {
        Toast.makeText(this, "'사랑해' 보내기 퀘스트 완료!", Toast.LENGTH_SHORT).show()
        val prefs = getSharedPreferences("questPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("quest_love_done", false)) {
            prefs.edit()
                .putBoolean("quest_love_done", true)
                .apply()
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
                
                // 새로운 메시지만 감지하여 감정 분석 수행
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        change.document.toObject(ChatMessage::class.java)?.let { message ->
                            // 상대방의 새로운 메시지인 경우에만 감정 분석 수행
                            if (message.senderUid != currentUserUid) {
                                android.util.Log.d("EmotionDebug", "새로운 상대방 메시지 감지: ${message.text}")
                                analyzeEmotion(message.text)
                            }
                        }
                    }
                }
                
                // 전체 메시지 목록 업데이트
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(ChatMessage::class.java)?.let { message ->
                        messages.add(message)
                    }
                }
                chatAdapter.updateMessages(messages)
                recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun analyzeEmotion(text: String) {
        // 긍정적인 단어들
        val positiveWords = listOf(
            "좋아", "행복", "즐거워", "웃겨", "재밌어", "최고", "사랑", "감사", "고마워", "응원", "하하", "ㅋㅋ", "ㅎㅎ",
            "멋져", "기뻐", "환상", "축하", "예뻐", "사랑해", "든든해", "대박", "완벽해", "미소", "힐링", "설레", "편안",
            "잘했어", "착해", "훈훈해", "신나", "기대돼", "감동", "화이팅", "행운", "감격", "의미 있어",
            "유쾌해", "반가워", "다정해", "따뜻해", "잘 지내", "재밌다", "짱", "존경", "귀여워", "소중", "친절", "천사",
            "믿음", "기특해", "좋은", "열정", "평화", "선물", "애기", "여보"
        )
        // 부정적인 단어들 (중복 제거됨)
        val negativeWords = listOf(
            "싫어", "화나", "짜증", "힘들", "슬퍼", "우울", "미워", "실망", "불만", "ㅠㅠ", "ㅜㅜ", "흑",
            "답답", "무서워", "지쳤", "괴로워", "불안", "눈물", "괴롭", "속상", "외로워", "외롭", "고통", "포기해",
            "멘붕", "후회돼", "지겹다", "절망", "좌절", "실패", "외면", "비참해", "실수", "무기력", "불쾌",
            "상처", "비난", "지옥", "한숨", "혼란", "허무해", "낙담", "안돼", "나빠", "죽겠네", "죽고 싶냐",
            "화났", "힘빠져", "억울해", "지치다", "헤어져", "끝이야"
        )

        // 초기화 단어 체크 (테스트용)
        val resetWords = listOf("초기화", "리셋", "처음으로", "원점", "다시")
        var shouldReset = false
        resetWords.forEach { word ->
            if (text.contains(word)) {
                shouldReset = true
                return@forEach
            }
        }

        if (shouldReset) {
            emotionScore = 0
            updateEmotionStatus(emotionScore)
            return
        }

        // 텍스트에서 감정 점수 계산
        var score = 0
        var positiveCount = 0
        var negativeCount = 0
        
        positiveWords.forEach { word ->
            // 단어가 텍스트에 몇 번 나오는지 카운트 (정확한 문자열 매칭)
            val count = text.split(word).size - 1
            if (count > 0) {
                positiveCount += count
                score += count
            }
        }
        negativeWords.forEach { word ->
            // 단어가 텍스트에 몇 번 나오는지 카운트 (정확한 문자열 매칭)
            val count = text.split(word).size - 1
            if (count > 0) {
                negativeCount += count
                score -= count
            }
        }
        
        // 디버그 로그 (일시적으로 Toast 추가)
        android.util.Log.d("EmotionDebug", "메시지: '$text', 긍정: $positiveCount, 부정: $negativeCount, 최종스코어: $score, 누적스코어: ${emotionScore + score}")

        // 감정 점수 단순 누적 (시연용)
        emotionScore += score

        // 감정 상태에 따라 UI 업데이트
        updateEmotionStatus(emotionScore)
    }

    private fun updateEmotionStatus(score: Int) {
        val emotionState = when {
            score >= 2 -> EmotionState.HAPPY      // 긍정 단어 2개 이상
            score in -1..1 -> EmotionState.NEUTRAL // -1~1 범위 (NEUTRAL 유지)
            score >= -3 -> EmotionState.SAD       // 부정 단어 2~3개 (-2, -3)
            else -> EmotionState.ANGRY            // 부정 단어 4개 이상 (-4 이하)
        }

        // UI 업데이트는 메인 스레드에서 실행
        runOnUiThread {
            // 감정 텍스트 설정 (스코어 포함)
            emotionText.text = "${emotionState.text} (${score})"
            //emotionText.setTextColor(emotionState.backgroundColor)
            // 감정 원형 색상 설정
            val circleDrawable = emotionCircle.background as GradientDrawable
            circleDrawable.setColor(emotionState.backgroundColor)
            // 감정 텍스트 스타일
            emotionText.textSize = 16f
            //emotionText.setTypeface(null, android.graphics.Typeface.BOLD)
            emotionText.typeface = ResourcesCompat.getFont(this, R.font.bmjua)
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