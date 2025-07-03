package com.mycompany.lovegarden

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.airbnb.lottie.LottieAnimationView
import com.mycompany.lovegarden.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source


//와이파이 관련
fun Context.isInternetAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
    return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

class PlantCareActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        // DrawerLayout 초기화
        drawerLayout = findViewById(R.id.drawerLayout)

        findViewById<ImageView>(R.id.ivSpeechBubble).setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUid = currentUser.uid
            val myUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid)

            myUserRef.child("roomId").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val myRoomId = snapshot.getValue(String::class.java)
                    if (myRoomId.isNullOrEmpty()) {
                        startActivity(Intent(this@PlantCareActivity, CodeInputActivity::class.java))
                    } else {
                        val intent = Intent(this@PlantCareActivity, ChatActivity::class.java)
                        intent.putExtra("roomId", myRoomId)
                        startActivity(intent)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PlantCareActivity, "데이터베이스 오류: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }


        // 햄버거 메뉴 버튼 클릭 시 사이드 메뉴 열기
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<ImageView>(R.id.btnQuestList).setOnClickListener{
            startActivity(Intent(this@PlantCareActivity, QuestListActivity::class.java))
        }

        // 사이드 메뉴 항목들 클릭 리스너 설정
        setupSideMenuListeners()

        findUserRoomAndRender() // 🔥 핵심 로직
    }


    // firebace의 임시 데이터 사용 나중엔 유동적으로 받아오기
    //val roomId = "ABCD1234"


    private fun findUserRoomAndRender() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        // 해당 유저의 roomId 필드 값 읽기
        userRef.child("roomId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foundRoomId = snapshot.getValue(String::class.java)
                if (foundRoomId != null) {
                    renderPlantImage(foundRoomId)

                    findViewById<ImageButton>(R.id.btnWater).setOnClickListener {
                        showItemModal(foundRoomId, R.drawable.water_item, "item.wateritem", isCody = false)
                    }

                    findViewById<ImageButton>(R.id.btnSunlight).setOnClickListener {
                        showItemModal(foundRoomId, R.drawable.sun_item, "item.lightitem", isCody = false)
                    }

                    findViewById<ImageButton>(R.id.btnNutrient).setOnClickListener {
                        showItemModal(foundRoomId,
                            R.drawable.nutrient_item, "item.healthitem", isCody = false)
                    }

                    findViewById<ImageButton>(R.id.btnMore).setOnClickListener {
                        AlertDialog.Builder(this@PlantCareActivity)
                            .setTitle("코디 기능 준비 중")
                            .setMessage("코디 기능은 현재 준비 중입니다.")
                            .setPositiveButton("확인", null)
                            .show()
                    }
                } else {
                    Toast.makeText(this@PlantCareActivity, "해당 유저에 연결된 방 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PlantCareActivity, "방 정보 로딩 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderPlantImage(roomId: String) {
        val plantAnimView = findViewById<LottieAnimationView>(R.id.ivPlant)
        val db = FirebaseFirestore.getInstance()
        val roomRef = db.collection("rooms").document(roomId)

        roomRef.get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                try {
                    val exp = (snapshot.getLong("plant.experience") ?: throw Exception("경험치 누락")).toInt()

                    // 🔥 경험치 → 애니메이션 프레임 위치 지정
                    val frame = when {
                        exp < 10 -> 5
                        exp < 20 -> 10
                        exp < 30 -> 15
                        exp < 40 -> 20
                        exp < 50 -> 25
                        exp < 60 -> 30
                        exp < 70 -> 35
                        exp < 80 -> 40
                        else -> 45
                        //else -> 144
                    }

                    plantAnimView.setAnimation("plants/plant01.json")
                    plantAnimView.setProgress(frame / 144f) // 0.0 ~ 1.0 비율로 지정
                    plantAnimView.pauseAnimation()
                    plantAnimView.visibility = View.VISIBLE

                } catch (e: Exception) {
                    Toast.makeText(this, "🌱 식물 상태를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "⚠️ 식물 정보 로딩 실패", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showItemModal(roomId: String, iconRes: Int, firebasePath: String, isCody: Boolean) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.md_item_modal)
        dialog.setCancelable(true)

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val itemImage = dialog.findViewById<ImageButton>(R.id.itemimg)
        val itemText = dialog.findViewById<TextView>(R.id.textView)
        val closeBtn = dialog.findViewById<Button>(R.id.backbtn)

        itemImage.setImageResource(iconRes)
        itemText.text = "불러오는 중..."
        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()

        itemImage.setOnClickListener {
            dialog.dismiss()

            if (!isCody) {
                val db = FirebaseFirestore.getInstance()
                val roomRef = db.collection("rooms").document(roomId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(roomRef)

                    val itemCount = (snapshot.getLong(firebasePath) ?: 0).toInt()
                    val exp = (snapshot.getLong("plant.experience") ?: 0).toInt()

                    if (itemCount <= 0) {
                        throw Exception("아이템이 없습니다")
                    }

                    transaction.update(roomRef, firebasePath, itemCount - 1)
                    transaction.update(roomRef, "plant.experience", exp + 1)
                }
                    .addOnSuccessListener {
                        // 🔥 트랜잭션 성공 후에만 애니메이션 실행
                        val effectView = findViewById<ImageView>(R.id.ivEffect)
                        effectView.setImageResource(iconRes)
                        effectView.visibility = ImageView.VISIBLE

                        val animation = AnimationUtils.loadAnimation(this, R.anim.effect_scale)
                        effectView.startAnimation(animation)

                        animation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation) {}
                            override fun onAnimationEnd(animation: Animation) {
                                effectView.visibility = ImageView.GONE
                            }
                            override fun onAnimationRepeat(animation: Animation) {}
                        })
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "아이템이 없습니다", Toast.LENGTH_SHORT).show()
                    }
            }
        }


        // 파이어베이스 데이터 불러오기
        val db = FirebaseFirestore.getInstance()
        val roomRef = db.collection("rooms").document(roomId)

        // ⛔ 와이파이 안 되면 수량 표시도 하지 않고, 메시지만 출력
        if (!this@PlantCareActivity.isInternetAvailable()) {
            itemText.text = if (!isCody) "x -" else "알 수 없음"
            Toast.makeText(this, "⚠️ 네트워크 오류입니다", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            roomRef.get(Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    if (isCody) {

                        // 기존 코디 아이템 UI 갱신
                        val codyMap = snapshot.get(firebasePath) as? Map<*, *>
                        if (codyMap != null) {
                            val myitem = codyMap["myitem"] as? Boolean ?: false
                            val price = (codyMap["price"] as? Long)?.toInt() ?: 0
                            val wearing = codyMap["wearing"] as? Boolean ?: false

                            itemText.text = when {
                                myitem && wearing -> "착용 중"
                                myitem -> "보유 중"
                                else -> "가격: $price"
                            }
                        } else {
                            itemText.text = "코디 아이템 없음"
                        }

                    } else {
                        // 기존 아이템 수량 텍스트
                        val count = (snapshot.getLong(firebasePath) ?: return@addOnSuccessListener).toInt()
                        itemText.text = "x $count"
                    }

                }
                .addOnFailureListener {
                    itemText.text = if (!isCody) "x -" else "알 수 없음"
                    Toast.makeText(this, "⚠️ 네트워크 오류입니다", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            itemText.text = if (!isCody) "x -" else "알 수 없음"
            Toast.makeText(this, "⚠️ 네트워크 오류 (예외)", Toast.LENGTH_SHORT).show()
        }


    }

    private fun setupSideMenuListeners() {
        // 사이드 메뉴 닫기 버튼
        findViewById<ImageButton>(R.id.btnCloseMenu).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // 로그아웃 메뉴
        findViewById<TextView>(R.id.menuLogout).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // 로그아웃 다이얼로그 호출
            showLogoutDialog()
        }

        // 탈퇴하기 메뉴
        findViewById<TextView>(R.id.menuDeleteAccount).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // 탈퇴 확인 다이얼로그 호출
            showDeleteAccountDialog()
        }


        // 이용약관 메뉴
        findViewById<TextView>(R.id.menuTerms).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showPolicyDialog("이용약관", "https://veiled-seed-36c.notion.site/214a7f300f09801ab4defb16ac2f2fc8")
        }

        // 개인정보처리방침 메뉴
        findViewById<TextView>(R.id.menuPrivacyPolicy).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showPolicyDialog("개인정보처리방침", "https://veiled-seed-36c.notion.site/Privacy-Policy-214a7f300f0980cc9af8e7486d12bc4c")
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("네") { dialog, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                this.startActivity(intent)
                this.finish()
            }
            .setNegativeButton("아니요") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("회원 탈퇴")
            .setMessage("정말 탈퇴하시겠습니까?\n\n탈퇴 시 모든 데이터가 삭제되며, 복구할 수 없습니다.")
            .setPositiveButton("탈퇴하기") { dialog, _ ->
                dialog.dismiss()
                deleteUserAccount()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // 약관확인 웹 다이얼로그
    fun showPolicyDialog(title: String, url: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("웹사이트로 이동할까요?")
            .setPositiveButton("이동") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }


    private fun deleteUserAccount() {
        // 1. 유저 확인 및 UID 가져오기
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        
        // 로딩 표시
        Toast.makeText(this, "계정 삭제 중...", Toast.LENGTH_SHORT).show()

        // 2. Firestore 인스턴스 가져오기
        val db = FirebaseFirestore.getInstance()
        val realtimeDb = FirebaseDatabase.getInstance()

        // 3. 탈퇴 로그 기록용 데이터 준비
        val deleteLog = hashMapOf(
            "uid" to uid,
            "deletedAt" to Timestamp.now()
        )

        // 4. Firestore에서 사용자 데이터 삭제 (순차적으로 진행)
        db.collection("users").document(uid).delete()
            .addOnSuccessListener {
                // 5. Realtime Database에서 사용자 데이터 삭제
                realtimeDb.getReference("users").child(uid).removeValue()
                    .addOnSuccessListener {
                        // 6. 탈퇴 로그 기록
                        db.collection("deleted_users").document(uid).set(deleteLog)
                            .addOnSuccessListener {
                                // 7. Firebase Auth 계정 삭제 (마지막에 실행)
                                currentUser.delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                        
                                        // 8. LoginActivity로 이동
                                        val intent = Intent(this, LoginActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "계정 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "탈퇴 로그 기록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "실시간 데이터베이스 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "사용자 데이터 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
