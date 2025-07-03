package com.mycompany.lovegarden

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.mycompany.lovegarden.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class QuestListActivity : AppCompatActivity() {

    private lateinit var btnReceive: AppCompatButton   // 받기 버튼
    private lateinit var btnClose: TextView            // 닫기(퀘스트창 닫기) 버튼

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest_list)

        btnReceive = findViewById(R.id.get_button)
        btnClose   = findViewById(R.id.closequest_button)

        // 1) 초기 상태: 퀘스트 완료 전에는 버튼 비활성화
        btnReceive.isEnabled = false
        // SharedPreferences나 Firestore에서 퀘스트 완료 여부를 불러와
        // 이미 완료된 경우라면 활성화
        val done = getSharedPreferences("questPrefs", MODE_PRIVATE)
            .getBoolean("quest_love_done", false)
        if (done) btnReceive.isEnabled = true

        // 2) 받기 버튼 클릭 시 → Firestore rooms/{roomId}.item.healthitem +1
        btnReceive.setOnClickListener {
            // 중복 클릭 방지
            btnReceive.isEnabled = false

            // A) 현재 로그인된 유저의 roomId를 RealtimeDB에서 가져오기
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrEmpty()) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                btnReceive.isEnabled = true
                return@setOnClickListener
            }
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)

            userRef.child("roomId")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val roomId = snapshot.getValue(String::class.java)
                        if (roomId.isNullOrEmpty()) {
                            Toast.makeText(
                                this@QuestListActivity,
                                "방 정보가 없습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            btnReceive.isEnabled = true
                            return
                        }

                        // B) Firestore 트랜잭션: healthitem +1
                        val db = FirebaseFirestore.getInstance()
                        val roomDoc = db.collection("rooms").document(roomId)

                        db.runTransaction { tx ->
                            val ds = tx.get(roomDoc)
                            // null 방지용 Elvis
                            val cur = (ds.getLong("item.healthitem") ?: 0L)
                            tx.update(roomDoc, "item.healthitem", cur + 1)
                        }
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@QuestListActivity,
                                    "영양제 1개 받았습니다!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // 퀘스트도 마무리 처리
                                getSharedPreferences("questPrefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("quest_love_done", true)
                                    .apply()
                                // 필요하면 뒤로가기 or 화면 갱신
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this@QuestListActivity,
                                    "아이템 받기 실패: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                btnReceive.isEnabled = true
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@QuestListActivity,
                            "DB 오류: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        btnReceive.isEnabled = true
                    }
                })
        }

        // 닫기 버튼은 기존 방식 유지
        btnClose.setOnClickListener {
            finish()
        }
    }
}