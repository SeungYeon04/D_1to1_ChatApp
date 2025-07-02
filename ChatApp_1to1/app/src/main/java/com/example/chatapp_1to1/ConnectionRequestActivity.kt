package com.example.chatapp_1to1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
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

// 데이터 모델: 화면에 표시할 사용자 닉네임(username) 등을 담습니다.
data class ConnectionRequest(
    val senderUid: String = "",
    val username: String = ""    // sender의 username 표시용
)

// 커스텀 어댑터: request_item.xml 레이아웃을 인플레이트하여 각 항목을 구성합니다.
class ConnectionRequestAdapter(
    context: Context,
    private val requests: MutableList<ConnectionRequest>
) : ArrayAdapter<ConnectionRequest>(context, 0, requests) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.request_item, parent, false)

        val nameTextView = view.findViewById<TextView>(R.id.request_name)
        val acceptButton = view.findViewById<Button>(R.id.accept_button)
        val rejectButton = view.findViewById<Button>(R.id.reject_button)

        // 데이터 바인딩
        val request = requests[position]
        nameTextView.text = request.username

        acceptButton.setOnClickListener {
            Toast.makeText(context, "${request.username}님의 요청을 수락했습니다.", Toast.LENGTH_SHORT).show()

            val senderUid = request.senderUid
            val receiverUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firestore의 connectionRequests 문서를 "accepted"로 업데이트
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("connectionRequests")
                .whereEqualTo("senderUid", senderUid)
                .whereEqualTo("receiverUid", receiverUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("status", "accepted")
                    }
                    // TODO: 수락 알림이 가는 로직 추가
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "요청 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // 수락 처리와 채팅방 생성을 진행
            RoomCreator.acceptRequest(
                senderUid, receiverUid,
                FirebaseDatabase.getInstance().getReference("users"),
                firestore
            ) { roomId, error ->
                if (roomId != null) {
                    // 채팅방 생성 성공 시 ChatActivity로 전환
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("roomId", roomId)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "채팅방 생성 실패: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rejectButton.setOnClickListener {
            Toast.makeText(context, "${request.username}님의 요청을 거절했습니다.", Toast.LENGTH_SHORT).show()
            // Firestore의 connectionRequests 문서에서 status를 "rejected"로 업데이트
            val senderUid = request.senderUid
            val receiverUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("connectionRequests")
                .whereEqualTo("senderUid", senderUid)
                .whereEqualTo("receiverUid", receiverUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update("status", "rejected")
                    }
                    // TODO: 거절 알림이 가는 로직 추가
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "요청 거절 처리 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}

class ConnectionRequestsActivity : AppCompatActivity() {

    private lateinit var btnclose: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: ConnectionRequestAdapter
    private val requests: MutableList<ConnectionRequest> = mutableListOf()

    private val currentUid: String? by lazy {
        FirebaseAuth.getInstance().currentUser?.uid
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_request)

        if (currentUid.isNullOrEmpty()) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnclose = findViewById(R.id.closelist_button)
        listView = findViewById(R.id.request_list)
        adapter = ConnectionRequestAdapter(this, requests)
        listView.adapter = adapter

        btnclose.setOnClickListener {
            finish()
        }

        loadConnectionRequests()
    }

    // Firestore에서 현재 로그인 사용자가 수신한 pending 요청 목록을 불러옵니다.
    private fun loadConnectionRequests() {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("connectionRequests")
            .whereEqualTo("receiverUid", currentUid)
            .whereEqualTo("status", "pending") // pending 상태의 요청만 불러오기
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val senderUid = document.getString("senderUid")
                        if (!senderUid.isNullOrEmpty()) {
                            // sender의 username은 Realtime Database의 "users" 노드에 저장되어 있음
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(senderUid)
                                .child("username")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val username = snapshot.getValue(String::class.java) ?: "Unknown"
                                        val req = ConnectionRequest(senderUid, username)
                                        requests.add(req)
                                        runOnUiThread {
                                            adapter.notifyDataSetChanged()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("FirebaseDB", "데이터 읽기 실패: ${error.message}")
                                    }
                                })
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "connectionRequests 불러오기 실패: ${exception.message}")
            }
    }
}

// RoomCreator 객체: 수락 시 채팅방 생성 및 사용자의 연결 상태 업데이트
object RoomCreator {

    fun acceptRequest(
        senderUid: String,
        receiverUid: String,
        allUsersRef: DatabaseReference,
        firestore: FirebaseFirestore,
        callback: (roomId: String?, error: String?) -> Unit
    ) {
        // sender와 receiver의 정보를 각각 조회합니다.
        allUsersRef.child(senderUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(senderSnapshot: DataSnapshot) {
                if (!senderSnapshot.exists()) {
                    callback(null, "요청 보낸 사용자 데이터를 불러올 수 없습니다.")
                    return
                }
                val senderUser = senderSnapshot.getValue(UserModel::class.java)
                if (senderUser == null) {
                    callback(null, "요청 보낸 사용자 정보를 읽지 못했습니다.")
                    return
                }
                allUsersRef.child(receiverUid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(receiverSnapshot: DataSnapshot) {
                        if (!receiverSnapshot.exists()) {
                            callback(null, "수신자 데이터를 불러올 수 없습니다.")
                            return
                        }
                        val receiverUser = receiverSnapshot.getValue(UserModel::class.java)
                        if (receiverUser == null) {
                            callback(null, "수신자 정보를 읽지 못했습니다.")
                            return
                        }

                        // 두 uid를 사전순 정렬하여 고유 채팅방 ID 생성
                        val roomId = if (senderUid < receiverUid) {
                            "${senderUid}_$receiverUid"
                        } else {
                            "${receiverUid}_$senderUid"
                        }

                        // 채팅방 데이터 구성
                        val roomData = hashMapOf<String, Any>()
                        roomData["createdAt"] = FieldValue.serverTimestamp()

                        val usersMap = hashMapOf<String, Any>()
                        val senderInfo = hashMapOf(
                            "nickname" to senderUser.username,
                            "uid" to senderUser.code
                        )
                        val receiverInfo = hashMapOf(
                            "nickname" to receiverUser.username,
                            "uid" to receiverUser.code
                        )
                        usersMap[senderUid] = senderInfo
                        usersMap[receiverUid] = receiverInfo
                        roomData["users"] = usersMap

                        //item 및 plant 필드를 구성 . 아래는 첫스타트 데이터
                        roomData["item"] = hashMapOf(
                            "codyitem" to hashMapOf(
                                "myitem" to false,
                                "price" to 100
                            ),
                            "healthitem" to 5,
                            "lightitem" to 10,
                            "wateritem" to 10
                        )
                        roomData["plant"] = hashMapOf(
                            "experience" to 0,
                            "money" to 1000
                        )

                        // Firestore "rooms" 컬렉션에 채팅방 생성
                        firestore.collection("rooms").document(roomId)
                            .set(roomData)
                            .addOnSuccessListener {
                                // 채팅방 생성 후 양쪽 사용자 Realtime Database의 "partnerUid"와 "roomId" 필드를 업데이트함
                                allUsersRef.child(senderUid).child("partnerUid").setValue(receiverUid)
                                allUsersRef.child(senderUid).child("roomId").setValue(roomId)
                                allUsersRef.child(receiverUid).child("partnerUid").setValue(senderUid)
                                allUsersRef.child(receiverUid).child("roomId").setValue(roomId)
                                callback(roomId, null)
                            }
                            .addOnFailureListener { e ->
                                callback(null, e.message ?: "채팅방 생성 실패")
                            }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        callback(null, error.message)
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {
                callback(null, error.message)
            }
        })
    }
}