package com.example.chatapp_1to1

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

class CodeInputActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codeinput)

        // Firebase 초기화
        database = FirebaseDatabase.getInstance().reference

        val etCode = findViewById<EditText>(R.id.etCodeInput)
        val btnTest = findViewById<Button>(R.id.btnJoin)

        btnTest.setOnClickListener {
            val code = etCode.text.toString().trim()
            
            if (code.isEmpty()) {
                Toast.makeText(this, "코드를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 테스트용 UID 사용
            val uid = "test_user_123"
            val chatRoomRef = database.child("chatRooms").child(code)

            // 채팅방 존재 여부 확인
            chatRoomRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // 채팅방이 없으면 새로 생성
                    val newChatRoom = mapOf(
                        "createdAt" to System.currentTimeMillis(),
                        "users" to mapOf(uid to true)
                    )
                    
                    chatRoomRef.setValue(newChatRoom)
                        .addOnSuccessListener {
                            // 채팅방 생성 후 첫 메시지 저장
                            val message = mapOf(
                                "sender" to uid,
                                "text" to "채팅방이 생성되었습니다",
                                "timestamp" to System.currentTimeMillis()
                            )

                            chatRoomRef.child("messages").push().setValue(message)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "새로운 채팅방이 생성되었습니다!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "메시지 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "채팅방 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // 기존 채팅방에 참여
                    chatRoomRef.child("users").child(uid).setValue(true)
                        .addOnSuccessListener {
                            val message = mapOf(
                                "sender" to uid,
                                "text" to "새로운 사용자가 참여했습니다",
                                "timestamp" to System.currentTimeMillis()
                            )

                            chatRoomRef.child("messages").push().setValue(message)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "채팅방 참여 완료!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "메시지 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "채팅방 참여 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "채팅방 확인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
