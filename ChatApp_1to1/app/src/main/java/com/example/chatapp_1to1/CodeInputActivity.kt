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

    private lateinit var myCodeText: TextView
    private lateinit var etCode: EditText
    private lateinit var btnJoin: Button
    private lateinit var allUsersRef: DatabaseReference
    private lateinit var myUsersRef: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codeinput)

        etCode = findViewById(R.id.etCodeInput)
        btnJoin = findViewById(R.id.btnJoin)
        mAuth = FirebaseAuth.getInstance()
        allUsersRef = FirebaseDatabase.getInstance().getReference("users")
        firestore = FirebaseFirestore.getInstance()
        myCodeText = findViewById(R.id.my_code_text)

        val currentUser = FirebaseAuth.getInstance().currentUser
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
            // 사용자가 로그인되어 있지 않은 경우 예외 처리(예: 로그인 화면으로 이동)
            myCodeText.text = "로그인이 필요합니다."
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

                        // 현재 사용자의 정보를 "users" 노드에서 조회
                        allUsersRef.child(currentUid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    Toast.makeText(this@CodeInputActivity, "현재 사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                val currentUserModel = snapshot.getValue(UserModel::class.java)
                                if (currentUserModel == null) {
                                    Toast.makeText(this@CodeInputActivity, "현재 사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                // 두 uid를 사전순으로 결합하여 채팅방 고유 ID 생성
                                val roomId = if (currentUid < matchedUid!!) {
                                    "${currentUid}_$matchedUid"
                                } else {
                                    "${matchedUid}_$currentUid"
                                }

                                // Firestore에 저장할 채팅방 데이터 구성
                                val roomData = HashMap<String, Any>()
                                roomData["createdAt"] = FieldValue.serverTimestamp()

                                // "users" 필드 구성 (회원가입 시 저장된 username은 nickname, code는 uid로 사용)
                                val usersMap = HashMap<String, Any>()
                                val currentUserMap = hashMapOf(
                                    "nickname" to currentUserModel.username,
                                    "uid" to currentUserModel.code
                                )
                                usersMap[currentUid] = currentUserMap

                                val matchedUserMap = hashMapOf<String, Any>()
                                if (matchedUser != null) {
                                    matchedUserMap["nickname"] = matchedUser.username
                                    matchedUserMap["uid"] = matchedUser.code
                                    usersMap[matchedUid] = matchedUserMap
                                } else {
                                    Toast.makeText(this@CodeInputActivity, "매칭된 사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                roomData["users"] = usersMap

                                // "item" 필드 구성
                                val itemMap = hashMapOf<String, Any>(
                                    "codyitem" to hashMapOf(
                                        "myitem" to false,
                                        "price" to 100
                                    ),
                                    "healthitem" to 0,
                                    "lightitem" to 0,
                                    "wateritem" to 0
                                )
                                roomData["item"] = itemMap

                                // "plant" 필드 구성
                                val plantMap = hashMapOf(
                                    "experience" to 15,
                                    "money" to 10
                                )
                                roomData["plant"] = plantMap

                                // Cloud Firestore의 "rooms" 컬렉션에 채팅방 문서 생성
                                firestore.collection("rooms").document(roomId)
                                    .set(roomData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@CodeInputActivity, "채팅방이 생성되었습니다.", Toast.LENGTH_SHORT).show()
                                        // ChatActivity 시작
                                        val intent = Intent(this@CodeInputActivity, ChatActivity::class.java)
                                        intent.putExtra("roomId", roomId)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@CodeInputActivity, "채팅방 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@CodeInputActivity,
                                    "현재 사용자 정보를 불러오는 중 오류: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    } else {
                        Toast.makeText(this@CodeInputActivity, "입력한 코드와 일치하는 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@CodeInputActivity,
                        "사용자 검색 중 오류: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}
