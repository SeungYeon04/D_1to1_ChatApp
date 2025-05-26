package com.example.chatapp_1to1

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

        // firebace의 임시 데이터 사용 나중엔 유동적으로 받아오기
        val roomId = "ABCD1234"

        // 💧 물 버튼 (이미지 : R.drawable.water_item.png)
        findViewById<ImageButton>(R.id.btnWater).setOnClickListener {
            showItemModal(roomId, R.drawable.water_item, "item.wateritem", isCody = false)
        }

        // ☀️ 햇빛 버튼
        findViewById<ImageButton>(R.id.btnSunlight).setOnClickListener {
            showItemModal(roomId, R.drawable.sun_item, "item.lightitem", isCody = false)
        }

        // 🌿 영양제 버튼
        findViewById<ImageButton>(R.id.btnNutrient).setOnClickListener {
            showItemModal(roomId, R.drawable.nutrient_item, "item.healthitem", isCody = false)
        }

        // 👕 코디 버튼 (예: 모자)
        findViewById<ImageButton>(R.id.btnMore).setOnClickListener {
            showItemModal(roomId, R.drawable.nutrient_item, "item.codyitem", isCody = true)
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
            roomRef.get(Source.SERVER) // 오프라인 캐시 무시하고 서버에서만 가져옴
                .addOnSuccessListener { snapshot ->
                    if (isCody) {
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
}
