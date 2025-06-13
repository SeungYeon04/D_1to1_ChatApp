package com.example.chatapp_1to1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CodeInputActivity : AppCompatActivity() {

    private lateinit var btnclose: TextView
    private lateinit var myCodeText: TextView
    private lateinit var etCode: EditText
    private lateinit var btnlist: Button
    private lateinit var btnJoin: Button
    private lateinit var allUsersRef: DatabaseReference
    private lateinit var myUsersRef: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codeinput)

        btnclose = findViewById(R.id.closecode_button)
        btnlist = findViewById(R.id.list_open_button)
        etCode = findViewById(R.id.etCodeInput)
        btnJoin = findViewById(R.id.btnJoin)
        myCodeText = findViewById(R.id.my_code_text)
        mAuth = FirebaseAuth.getInstance()
        allUsersRef = FirebaseDatabase.getInstance().getReference("users")
        firestore = FirebaseFirestore.getInstance()

        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            myUsersRef = allUsersRef.child(uid)

            myUsersRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val code = snapshot.child("code").getValue(String::class.java)
                    myCodeText.text = code ?: "코드 생성이 되지 않았습니다."
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@CodeInputActivity,
                        "내 코드 확인 중 오류: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            myCodeText.text = "로그인이 필요합니다."
        }

        btnlist.setOnClickListener {
            val intent = Intent(this, ConnectionRequestsActivity::class.java)
            startActivity(intent)
        }

        btnJoin.setOnClickListener {
            val inputCode = etCode.text.toString().trim()
            if (inputCode.length != 12) {
                Toast.makeText(this, "12자리 코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 현재 로그인한 사용자 정보 확인
            val currentUser = mAuth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUid = currentUser.uid

            // Realtime Database의 "users"에서 입력한 code와 일치하는 사용자를 검색
            val query = allUsersRef.orderByChild("code").equalTo(inputCode)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var matchedSnapshot: DataSnapshot? = null
                        for (child in snapshot.children) {
                            matchedSnapshot = child
                            break // 첫 번째 매칭 사용자 선택
                        }
                        if (matchedSnapshot == null) {
                            Toast.makeText(this@CodeInputActivity, "일치하는 사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            return
                        }

                        val matchedUser = matchedSnapshot.getValue(UserModel::class.java)
                        val matchedUid = matchedSnapshot.key

                        // 자신의 코드는 입력할 수 없도록 검사
                        if (matchedUid == currentUid) {
                            Toast.makeText(this@CodeInputActivity, "자신의 코드는 입력할 수 없습니다.", Toast.LENGTH_SHORT).show()
                            return
                        }

                        // 연결 요청 데이터를 Firestore에 저장
                        val requestData = hashMapOf(
                            "senderUid" to currentUid,
                            "receiverUid" to matchedUid,
                            "status" to "pending",
                            "timestamp" to FieldValue.serverTimestamp()
                        )

                        firestore.collection("connectionRequests")
                            .add(requestData)
                            .addOnSuccessListener {
                                Toast.makeText(this@CodeInputActivity, "연결 요청이 전송되었습니다.", Toast.LENGTH_SHORT).show()
                                // TODO: 신청 받은 사람에게 알림이 갈 수 있도록 로직 추가
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@CodeInputActivity, "요청 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this@CodeInputActivity, "입력한 코드와 일치하는 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CodeInputActivity, "사용자 검색 중 오류: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnclose.setOnClickListener {
            var Intent = Intent(applicationContext, PlantCareActivity::class.java)
            startActivity(Intent)
            finish()
        }
    }
}