package com.example.chatapp_1to1

import android.content.Context
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

// 데이터 모델: 화면에 표시할 사용자 닉네임(username)을 담습니다.
data class ConnectionRequest(val username: String)

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
            // TODO: status 필드를 "accepted"로 업데이트 + 계정 연결 및 채팅방 생성 + 상대측에게 수락 알림 로직 추가
        }

        rejectButton.setOnClickListener {
            Toast.makeText(context, "${request.username}님의 요청을 거절했습니다.", Toast.LENGTH_SHORT).show()
            // TODO: status 필드를 "rejected"로 업데이트 + 상대측에게 거절 알림 로직 추가
        }

        return view
    }
}


class ConnectionRequestsActivity : AppCompatActivity() {

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

        listView = findViewById(R.id.request_list)
        adapter = ConnectionRequestAdapter(this, requests)
        listView.adapter = adapter

        loadConnectionRequests()
    }

    private fun loadConnectionRequests() {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("connectionRequests")
            .whereEqualTo("receiverUid", currentUid)
            .whereEqualTo("status", "pending") // pending 상태의 요청만 불러옵니다.
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val senderUid = document.getString("senderUid")
                        if (!senderUid.isNullOrEmpty()) {
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(senderUid)
                                .child("username")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val username = snapshot.getValue(String::class.java) ?: "Unknown"
                                        requests.add(ConnectionRequest(username))
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