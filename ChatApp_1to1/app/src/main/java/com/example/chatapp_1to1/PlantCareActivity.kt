package com.example.chatapp_1to1

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        findViewById<ImageView>(R.id.ivSpeechBubble).setOnClickListener {
            startActivity(Intent(this, CodeInputActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            showLogoutDialog(this)
        }

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

        val db = FirebaseFirestore.getInstance()
        db.collection("rooms").get()
            .addOnSuccessListener { querySnapshot ->
                var foundRoomId: String? = null
                for (doc in querySnapshot) {
                    val users = doc.get("users") as? Map<*, *> ?: continue
                    for ((_, value) in users) {
                        val userMap = value as? Map<*, *> ?: continue
                        //Log.d("DEBUG_CHECK", "userMap uid=${userMap["uid"]}, my uid=$uid")
                        if (userMap["uid"] == uid.toString()) {
                            foundRoomId = doc.id
                            //Toast.makeText(this, "찾은 방 ID: $foundRoomId", Toast.LENGTH_LONG).show()
                            break
                        }
                    }
                    if (foundRoomId != null) break
                }

                if (foundRoomId != null) {
                    renderPlantImage(foundRoomId)

                    findViewById<ImageButton>(R.id.btnWater).setOnClickListener {
                        showItemModal(foundRoomId, R.drawable.water_item, "item.wateritem", isCody = false)
                    }

                    findViewById<ImageButton>(R.id.btnSunlight).setOnClickListener {
                        showItemModal(foundRoomId, R.drawable.sun_item, "item.lightitem", isCody = false)
                    }

                    findViewById<ImageButton>(R.id.btnNutrient).setOnClickListener {
                        showItemModal(foundRoomId, R.drawable.nutrient_item, "item.healthitem", isCody = false)
                    }

                    findViewById<ImageButton>(R.id.btnMore).setOnClickListener {
                        showItemModal(foundRoomId, R.drawable.nutrient_item, "item.codyitem", isCody = true)
                    }

                } else {
                    Toast.makeText(this, "해당 유저가 속한 방을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "방 정보 로딩 실패", Toast.LENGTH_SHORT).show()
            }
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
                        exp < 3 -> 20
                        exp < 5 -> 40
                        exp < 10 -> 60
                        exp < 20 -> 90
                        exp < 30 -> 120
                        else -> 144
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

    fun PlantCareActivity.showLogoutDialog(activity: PlantCareActivity) {
        AlertDialog.Builder(activity)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("네") { dialog, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(activity, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("아니요") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
